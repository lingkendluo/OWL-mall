<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '@shared/http'
import SubPage from '@/components/SubPage.vue'
import AppToast from '@/components/AppToast.vue'
import { useToast } from '@/composables/useToast'
import { orderApi, type OrderBrief, type OrderStatusFilter } from '@/services/orderApi'
import { money } from '@/utils/format'
import './orders.css'

/**
 * 我的订单。数据全部来自后端 /trade/orders，下单后立刻能在这里看到待付款。
 * 状态值与 OrderStatus 枚举一致：10 待付款 / 20 待发货 / 30 待收货 / 40 已完成 / 50 已取消。
 */
const TABS: Array<{ key: OrderStatusFilter; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'unpaid', label: '待付款' },
  { key: 'undelivered', label: '待发货' },
  { key: 'shipping', label: '待收货' },
  { key: 'finished', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
]

const route = useRoute()
const router = useRouter()
const { show } = useToast()

/** 从个人中心点「待付款」进来时带 ?status=unpaid，直接落到对应筛选 */
const initialStatus = route.query.status
const tab = ref<OrderStatusFilter>(
  TABS.some((item) => item.key === initialStatus) ? (initialStatus as OrderStatusFilter) : 'all',
)
const rows = ref<OrderBrief[]>([])
const total = ref(0)
const page = ref(1)
const pages = ref(1)
const loading = ref(false)
const error = ref('')
const PAGE_SIZE = 10

const subtitle = computed(() =>
  loading.value ? '正在读取订单…' : total.value ? `共 ${total.value} 单` : '还没有订单',
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await orderApi.list(tab.value, page.value, PAGE_SIZE)
    rows.value = result.list
    total.value = result.total
    pages.value = Math.max(1, result.pages)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '订单加载失败'
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function pickTab(key: OrderStatusFilter) {
  if (tab.value === key) return
  tab.value = key
  page.value = 1
  void load()
}

function goPage(next: number) {
  if (next < 1 || next > pages.value || next === page.value) return
  page.value = next
  void load()
}

async function cancel(order: OrderBrief) {
  if (!window.confirm(`取消订单 ${order.orderNo}？库存会退回，取消后不可恢复。`)) return
  try {
    await orderApi.cancel(order.orderNo)
    show('订单已取消，库存已退回')
    await load()
  } catch (e) {
    show(e instanceof ApiError ? e.message : '取消失败，请稍后重试')
  }
}

function goPay() {
  show('支付通道还没接，先去管理端看这单吧')
}

/** 待付款订单只剩多少时间，过期后端会自动关单 */
function leftTime(at: number | null): string {
  if (!at) return ''
  const diff = at - Date.now()
  if (diff <= 0) return '已超时'
  const minutes = Math.ceil(diff / 60000)
  return minutes >= 60 ? `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分` : `${minutes} 分钟`
}

function stamp(at: number): string {
  if (!at) return '—'
  const date = new Date(at)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function statusClass(status: number): string {
  if (status === 10) return 'is-unpaid'
  if (status === 20) return 'is-undelivered'
  if (status === 30) return 'is-shipping'
  if (status === 40) return 'is-finished'
  return 'is-off'
}

onMounted(load)
</script>

<template>
  <SubPage title="我的订单" :subtitle="subtitle">
    <div class="ord-tabs">
      <button
        v-for="item in TABS"
        :key="item.key"
        class="ord-tab"
        :class="{ 'is-active': tab === item.key }"
        type="button"
        @click="pickTab(item.key)"
      >
        {{ item.label }}
      </button>
    </div>

    <div v-if="error" class="ord-alert">
      {{ error }}
      <button type="button" @click="load">重试</button>
    </div>

    <div v-if="loading" class="ord-empty"><b>正在读取订单…</b></div>

    <div v-else-if="rows.length === 0" class="ord-empty">
      <span class="ord-empty-ico">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M6.4 3.8h11.2v16.4l-2.8-1.6-2.8 1.6-2.8-1.6-2.8 1.6V3.8Z" />
          <path d="M9.4 8.4h5.2M9.4 12h5.2" />
        </svg>
      </span>
      <b>这个状态下没有订单</b>
      <p>去商城挑点东西，或者换个状态看看</p>
      <button class="ord-empty-btn" type="button" @click="router.push({ name: 'home' })">
        去逛逛
      </button>
    </div>

    <ul v-else class="ord-list">
      <li v-for="order in rows" :key="order.orderNo" class="ord-card">
        <header class="ord-head">
          <span class="ord-no">{{ order.orderNo }}</span>
          <em class="ord-status" :class="statusClass(order.status)">{{ order.statusText }}</em>
        </header>

        <div class="ord-body">
          <div class="ord-thumb">
            <img
              v-if="order.coverImage"
              :src="order.coverImage"
              :alt="order.firstItemName"
              loading="lazy"
              decoding="async"
            />
            <span v-else>🛍️</span>
          </div>
          <div class="ord-info">
            <p class="ord-name">{{ order.firstItemName }}</p>
            <p class="ord-meta">共 {{ order.totalQty }} 件 · {{ stamp(order.createdAt) }}</p>
            <p v-if="order.status === 10 && order.expireAt" class="ord-expire">
              剩余 {{ leftTime(order.expireAt) }} 自动关闭
            </p>
          </div>
          <p class="ord-amount"><small>¥</small>{{ money(order.payAmount) }}</p>
        </div>

        <footer class="ord-foot">
          <button
            v-if="order.status === 10"
            class="ord-btn is-ghost"
            type="button"
            @click="cancel(order)"
          >
            取消订单
          </button>
          <button v-if="order.status === 10" class="ord-btn" type="button" @click="goPay">
            去支付
          </button>
          <span v-else class="ord-foot-note">有疑问？联系客服</span>
        </footer>
      </li>
    </ul>

    <div v-if="pages > 1" class="ord-pager">
      <button type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span>第 {{ page }} / {{ pages }} 页</span>
      <button type="button" :disabled="page >= pages" @click="goPage(page + 1)">下一页</button>
    </div>
  </SubPage>

  <AppToast />
</template>
