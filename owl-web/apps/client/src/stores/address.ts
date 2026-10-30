import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { ApiError } from '@shared/http'
import { addressApi, type ApiAddress, type AddressPayload } from '@/services/addressApi'
import { useAuthStore } from './auth'

/**
 * 收货地址簿（接后端 /m/addresses）。
 *
 * 页面仍然用「浙江省 杭州市 西湖区」一个输入框，后端要 province/city/district 三段，
 * 转换集中在 toAddress / toPayload 里做，视图一行都不用改。
 *
 * 登录后自动拉取，换账号清空重拉；未登录时列表为空（下单本来也要求登录）。
 */

/** 常用标签，前端只用这三个 + 其它 */
export const ADDRESS_TAGS = ['家', '公司', '学校'] as const

export interface Address {
  id: string
  /** 收货人 */
  name: string
  phone: string
  /** 省 / 市 / 区，用空格分隔，例：浙江省 杭州市 西湖区 */
  region: string
  /** 街道门牌 */
  detail: string
  /** 家 / 公司 / 学校 / 其它 */
  tag: string
  isDefault: boolean
  createdAt: number
}

/** 表单提交体：默认标记单独处理 */
export type AddressInput = Omit<Address, 'id' | 'createdAt'>

/** 「浙江省 杭州市 西湖区」→ 三段；后端三个字段都必填，所以区县不能缺 */
export function splitRegion(region: string): { province: string; city: string; district: string } {
  const parts = region.trim().replace(/\s+/g, ' ').split(' ').filter(Boolean)
  return {
    province: parts[0] ?? '',
    city: parts[1] ?? '',
    district: parts.slice(2).join(' ') || parts[1] || '',
  }
}

export function joinRegion(province: string, city: string, district: string): string {
  return [province, city, district].filter(Boolean).join(' ')
}

function toAddress(row: ApiAddress): Address {
  return {
    id: row.id,
    name: row.receiver,
    phone: row.phone,
    region: joinRegion(row.province, row.city, row.district),
    detail: row.detail,
    tag: row.tag || '家',
    isDefault: row.isDefault === true,
    createdAt: 0,
  }
}

function toPayload(input: AddressInput): AddressPayload {
  const { province, city, district } = splitRegion(input.region)
  return {
    receiver: input.name,
    phone: input.phone,
    province,
    city,
    district,
    detail: input.detail,
    tag: input.tag,
    isDefault: input.isDefault,
  }
}

export const useAddressStore = defineStore('address', () => {
  const auth = useAuthStore()

  const list = ref<Address[]>([])
  const loading = ref(false)
  const loaded = ref(false)
  const error = ref('')

  async function load(force = false): Promise<void> {
    if (!auth.isLoggedIn) {
      list.value = []
      loaded.value = false
      return
    }
    if (loading.value) return
    if (loaded.value && !force) return

    loading.value = true
    error.value = ''
    try {
      const rows = await addressApi.list()
      list.value = rows.map(toAddress)
      loaded.value = true
    } catch (e) {
      error.value = e instanceof ApiError ? e.message : '地址加载失败'
      list.value = []
    } finally {
      loading.value = false
    }
  }

  // 登录成谁就拉谁的地址；退出（id 变空）立刻清干净
  watch(
    () => auth.user?.id,
    (id) => {
      list.value = []
      loaded.value = false
      if (id) void load()
    },
    { immediate: true },
  )

  /** 默认地址；万一一个都没标默认，就拿第一条兜底 */
  const defaultAddress = computed(
    () => list.value.find((item) => item.isDefault) ?? list.value[0] ?? null,
  )

  const count = computed(() => list.value.length)
  const empty = computed(() => loaded.value && list.value.length === 0)

  function find(id: string): Address | null {
    return list.value.find((item) => item.id === id) ?? null
  }

  async function add(input: AddressInput): Promise<Address> {
    const created = await addressApi.create(toPayload(input))
    await load(true)
    return toAddress(created)
  }

  async function update(id: string, input: AddressInput): Promise<Address | null> {
    const updated = await addressApi.update(id, toPayload(input))
    await load(true)
    return toAddress(updated)
  }

  async function remove(id: string): Promise<void> {
    await addressApi.remove(id)
    await load(true)
  }

  async function setDefault(id: string): Promise<void> {
    await addressApi.setDefault(id)
    await load(true)
  }

  return { list, loading, loaded, error, count, empty, defaultAddress, load, add, update, remove, setDefault, find }
})
