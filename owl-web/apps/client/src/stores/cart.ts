import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { round2 } from '@/utils/format'

/** 满这个金额免运费，与顶栏配送条文案保持一致 */
export const FREE_SHIPPING = 99
/** 单行最大数量，防止步进加到离谱 */
export const MAX_QTY = 99

const STORAGE_KEY = 'owl.cart.v1'

/** 购物车行：存商品快照而不是引用，本地持久化后即使目录数据变了也不会炸 */
export interface CartLine {
  id: string
  name: string
  spec: string
  price: number
  /** 划线价 */
  list: number
  emoji: string
  /** 商品主图 URL，没有则回落到 emoji */
  image?: string
  qty: number
  checked: boolean
}

export type CartInput = Omit<CartLine, 'qty' | 'checked'>

function restore(): CartLine[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.flatMap((row): CartLine[] => {
      if (!row || typeof row !== 'object') return []
      const r = row as Record<string, unknown>
      const price = Number(r.price)
      const qty = Number(r.qty)
      if (typeof r.id !== 'string' || !r.id || !Number.isFinite(price)) return []
      return [
        {
          id: r.id,
          name: String(r.name ?? ''),
          spec: String(r.spec ?? ''),
          price,
          list: Number.isFinite(Number(r.list)) ? Number(r.list) : price,
          emoji: String(r.emoji ?? '🛍️'),
          image: typeof r.image === 'string' ? r.image : undefined,
          qty: Number.isFinite(qty) ? Math.min(MAX_QTY, Math.max(1, Math.round(qty))) : 1,
          checked: r.checked !== false,
        },
      ]
    })
  } catch {
    return []
  }
}

export const useCartStore = defineStore('cart', () => {
  const lines = ref<CartLine[]>(restore())

  // 深层监听，任何数量/勾选变化都落盘
  watch(
    lines,
    (value) => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
      } catch {
        /* 隐身模式写不进去，忽略即可 */
      }
    },
    { deep: true },
  )

  const count = computed(() => lines.value.reduce((sum, line) => sum + line.qty, 0))
  const amount = computed(() => round2(lines.value.reduce((sum, l) => sum + l.price * l.qty, 0)))
  /** 相对划线价省下的钱 */
  const saved = computed(() =>
    round2(lines.value.reduce((sum, l) => sum + Math.max(0, l.list - l.price) * l.qty, 0)),
  )

  const checkedLines = computed(() => lines.value.filter((line) => line.checked))
  const checkedCount = computed(() => checkedLines.value.reduce((sum, l) => sum + l.qty, 0))
  const checkedAmount = computed(() =>
    round2(checkedLines.value.reduce((sum, l) => sum + l.price * l.qty, 0)),
  )
  const allChecked = computed(
    () => lines.value.length > 0 && lines.value.every((line) => line.checked),
  )

  const empty = computed(() => lines.value.length === 0)
  const isFreeShipping = computed(() => checkedAmount.value >= FREE_SHIPPING)
  /** 还差多少免运费 */
  const toFreeShipping = computed(() =>
    round2(Math.max(0, FREE_SHIPPING - checkedAmount.value)),
  )
  const shippingRate = computed(() =>
    Math.min(100, Math.round((checkedAmount.value / FREE_SHIPPING) * 100)),
  )

  function add(input: CartInput, qty = 1) {
    const hit = lines.value.find((line) => line.id === input.id)
    if (hit) {
      hit.qty = Math.min(MAX_QTY, hit.qty + qty)
      hit.checked = true
      return
    }
    lines.value.unshift({ ...input, qty: Math.min(MAX_QTY, Math.max(1, qty)), checked: true })
  }

  function remove(id: string) {
    lines.value = lines.value.filter((line) => line.id !== id)
  }

  function setQty(id: string, qty: number) {
    const hit = lines.value.find((line) => line.id === id)
    if (!hit) return
    hit.qty = Math.min(MAX_QTY, Math.max(1, Math.round(qty)))
  }

  function increase(id: string) {
    const hit = lines.value.find((line) => line.id === id)
    if (hit) hit.qty = Math.min(MAX_QTY, hit.qty + 1)
  }

  function decrease(id: string) {
    const hit = lines.value.find((line) => line.id === id)
    if (hit && hit.qty > 1) hit.qty -= 1
  }

  function toggle(id: string) {
    const hit = lines.value.find((line) => line.id === id)
    if (hit) hit.checked = !hit.checked
  }

  function toggleAll() {
    const next = !allChecked.value
    for (const line of lines.value) line.checked = next
  }

  function removeChecked() {
    lines.value = lines.value.filter((line) => !line.checked)
  }

  /** 结算时整批搬去待购买清单 */
  function removeMany(ids: string[]) {
    const drop = new Set(ids)
    lines.value = lines.value.filter((line) => !drop.has(line.id))
  }

  function clear() {
    lines.value = []
  }

  return {
    lines,
    count,
    amount,
    saved,
    checkedLines,
    checkedCount,
    checkedAmount,
    allChecked,
    empty,
    isFreeShipping,
    toFreeShipping,
    shippingRate,
    add,
    remove,
    setQty,
    increase,
    decrease,
    toggle,
    toggleAll,
    removeChecked,
    removeMany,
    clear,
  }
})
