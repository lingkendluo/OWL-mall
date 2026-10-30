<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SubPage from '@/components/SubPage.vue'
import AppToast from '@/components/AppToast.vue'
import { useToast } from '@/composables/useToast'
import { orderApi, type OrderCounts } from '@/services/orderApi'
import { useAuthStore, maskPhone } from '@/stores/auth'
import { useAddressStore } from '@/stores/address'
import { useCartStore } from '@/stores/cart'
import { useMessageStore } from '@/stores/messages'
import { money } from '@/utils/format'
import './profile.css'

const router = useRouter()
const auth = useAuthStore()
const address = useAddressStore()
const cart = useCartStore()
const messages = useMessageStore()
const { show } = useToast()

const ICONS = {
  owl: '<path d="M7 11.4 6.6 5.6l4.7 3.6"/><path d="M25 11.4l.4-5.8-4.7 3.6"/><path d="M16 6.6c5.6 0 9.4 4.3 9.4 9.7s-4 10.9-9.4 10.9-9.4-5.5-9.4-10.9S10.4 6.6 16 6.6Z"/><path d="M11.9 14.4a3.5 3.5 0 1 0 0 0Z"/><path d="M20.1 14.4a3.5 3.5 0 1 0 0 0Z"/><path d="M16 17.6l-1.4 2.2h2.8L16 17.6Z"/>',
  wallet:
    '<path d="M4 7.6A2.4 2.4 0 0 1 6.4 5.2h9.2"/><path d="M4 7.6h13.6A2.4 2.4 0 0 1 20 10v7a2.4 2.4 0 0 1-2.4 2.4H6.4A2.4 2.4 0 0 1 4 17V7.6Z"/><path d="M16.2 13.4h1.6"/>',
  ticket:
    '<path d="M4 9.6V7.2h16v2.4a2.4 2.4 0 0 0 0 4.8v2.4H4v-2.4a2.4 2.4 0 0 0 0-4.8Z"/><path d="M12 8.4v7.2"/>',
  star: '<path d="M12 4.6 14 9l4.7.5-3.5 3.2 1 4.7-4.2-2.4-4.2 2.4 1-4.7L5.3 9.5 10 9Z"/>',
  pin: '<path d="M12 21c4.2-4 6.4-7 6.4-9.8A6.4 6.4 0 0 0 5.6 11.2C5.6 14 7.8 17 12 21Z"/><path d="M12 13.4a2.2 2.2 0 1 0 0-4.4 2.2 2.2 0 0 0 0 4.4Z"/>',
  heart:
    '<path d="M12 19.5S4.8 15 4.8 10.4a3.8 3.8 0 0 1 7.2-1.7 3.8 3.8 0 0 1 7.2 1.7c0 4.6-7.2 9.1-7.2 9.1Z"/>',
  clock: '<path d="M12 20.4a8.4 8.4 0 1 0 0-16.8 8.4 8.4 0 0 0 0 16.8Z"/><path d="M12 7.6V12l3 1.8"/>',
  receipt:
    '<path d="M6.4 3.8h11.2v16.4l-2.8-1.6-2.8 1.6-2.8-1.6-2.8 1.6V3.8Z"/><path d="M9.4 8.4h5.2"/><path d="M9.4 12h5.2"/>',
  headset:
    '<path d="M4.8 15.4v-3a7.2 7.2 0 0 1 14.4 0v3"/><path d="M4.8 14.2h1.8v4.4H6a1.2 1.2 0 0 1-1.2-1.2v-3.2Z"/><path d="M19.2 14.2H17.4v4.4H18a1.2 1.2 0 0 0 1.2-1.2v-3.2Z"/>',
  gear: '<path d="M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4Z"/><path d="M12 3.6v2.2M12 18.2v2.2M4.9 7.9l1.9 1.1M17.2 15l1.9 1.1M4.9 16.1l1.9-1.1M17.2 9l1.9-1.1"/>',
  chevron: '<path d="M9.5 6.5 15 12l-5.5 5.5"/>',
}

/* 用户信息统一从 auth store 来，登录成谁就显示谁；没登录时由路由守卫兜住 */
const profile = computed(() => {
  const current = auth.user
  return {
    name: current?.name ?? '未登录',
    level: auth.levelLabel,
    contact: auth.contact,
    note: current?.memberNo ? `会员编号 ${current.memberNo}` : '体验账号，数据不会保存',
    points: current?.points ?? 0,
    coupons: current?.coupons ?? 0,
    balance: current?.balance ?? 0,
  }
})

/** 订单角标全部来自后端 /trade/orders/counts，没拉到就都显示 0 */
const orderCounts = ref<OrderCounts | null>(null)

const ORDERS = computed(() => [
  { key: 'unpaid', label: '待付款', count: orderCounts.value?.unpaid ?? 0, icon: ICONS.wallet },
  { key: 'shipping', label: '待收货', count: orderCounts.value?.shipping ?? 0, icon: ICONS.clock },
  { key: 'review', label: '待评价', count: orderCounts.value?.review ?? 0, icon: ICONS.star },
  { key: 'after', label: '退换 / 售后', count: orderCounts.value?.after ?? 0, icon: ICONS.receipt },
])

/** 个人中心的四个入口映射到订单页的筛选项 */
function openOrders(key: string) {
  const map: Record<string, string> = {
    all: 'all',
    unpaid: 'unpaid',
    shipping: 'shipping',
    review: 'finished',
    after: 'all',
  }
  void router.push({ name: 'orders', query: { status: map[key] ?? 'all' } })
}

async function loadOrderCounts() {
  try {
    orderCounts.value = await orderApi.counts()
  } catch {
    // 后端没起时不显示角标，页面照常
    orderCounts.value = null
  }
}

onMounted(loadOrderCounts)

const assets = computed(() => [
  { key: 'coupon', label: '优惠券', value: `${profile.value.coupons} 张`, icon: ICONS.ticket },
  {
    key: 'points',
    label: '积分',
    value: profile.value.points.toLocaleString('zh-CN'),
    icon: ICONS.star,
  },
  { key: 'balance', label: '余额', value: `¥${money(profile.value.balance)}`, icon: ICONS.wallet },
])

const SERVICES = [
  { key: 'fav', label: '我的收藏', desc: '收藏的商品与店铺', icon: ICONS.heart },
  { key: 'history', label: '浏览记录', desc: '最近看过的 48 件商品', icon: ICONS.clock },
  { key: 'invoice', label: '发票管理', desc: '抬头管理、开票记录', icon: ICONS.receipt },
  { key: 'service', label: '售后服务', desc: '退换货、价保申请', icon: ICONS.headset },
  { key: 'setting', label: '设置', desc: '通知、隐私、账号安全', icon: ICONS.gear },
]

function todo(name: string) {
  show(`${name} 开发中`)
}

function openAddresses() {
  void router.push({ name: 'addresses' })
}

/* ---------------- 退出登录 ---------------- */
/* 不做二次确认弹窗，改成「点一次变成确认态，4 秒内再点才真退」 */
const quitArmed = ref(false)
let quitTimer = 0

async function onQuit() {
  if (!quitArmed.value) {
    quitArmed.value = true
    window.clearTimeout(quitTimer)
    quitTimer = window.setTimeout(() => {
      quitArmed.value = false
    }, 4000)
    return
  }

  window.clearTimeout(quitTimer)
  quitArmed.value = false
  await auth.signOut()
  void router.replace({ name: 'login', query: { logout: '1' } })
}

onBeforeUnmount(() => window.clearTimeout(quitTimer))

function goCart() {
  router.push({ name: 'cart' })
}

function goMessages() {
  router.push({ name: 'messages' })
}
</script>

<template>
  <SubPage title="个人中心" :subtitle="`${profile.level} · ${profile.contact}`">
    <div class="prof">
      <!-- 用户卡 -->
      <section class="prof-user">
        <span class="prof-avatar">
          <svg
            viewBox="0 0 32 32"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.owl"
          />
        </span>

        <div class="prof-meta">
          <h2>{{ profile.name }}</h2>
          <p class="prof-tags">
            <span class="prof-level">{{ profile.level }}</span>
            <span class="prof-note">{{ profile.note }}</span>
          </p>
        </div>

        <div class="prof-quick">
          <button type="button" @click="todo('会员码')">会员码</button>
          <button type="button" @click="goMessages">
            消息<em v-if="messages.unread">{{ messages.unread }}</em>
          </button>
          <button type="button" @click="goCart">
            购物车<em v-if="cart.count">{{ cart.count }}</em>
          </button>
        </div>
      </section>

      <!-- 资产三格 -->
      <section class="prof-assets">
        <button
          v-for="asset in assets"
          :key="asset.key"
          class="prof-asset"
          type="button"
          @click="todo(asset.label)"
        >
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="asset.icon"
          />
          <b>{{ asset.value }}</b>
          <span>{{ asset.label }}</span>
        </button>
      </section>

      <!-- 我的订单 -->
      <section class="prof-card">
        <header class="prof-card-head">
          <h2>我的订单</h2>
          <button type="button" class="prof-more" @click="openOrders('all')">
            全部订单
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              v-html="ICONS.chevron"
            />
          </button>
        </header>

        <div class="prof-orders">
          <button
            v-for="order in ORDERS"
            :key="order.key"
            class="prof-order"
            type="button"
            @click="openOrders(order.key)"
          >
            <span class="prof-order-ico">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.6"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="order.icon"
              />
              <em v-if="order.count">{{ order.count }}</em>
            </span>
            <span>{{ order.label }}</span>
          </button>
        </div>
      </section>

      <!-- 收货地址 -->
      <section class="prof-card">
        <header class="prof-card-head">
          <h2>收货地址</h2>
          <button type="button" class="prof-more" @click="openAddresses">
            管理
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              v-html="ICONS.chevron"
            />
          </button>
        </header>

        <button type="button" class="prof-addr" @click="openAddresses">
          <span class="prof-addr-ico">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
              v-html="ICONS.pin"
            />
          </span>
          <div v-if="address.defaultAddress">
            <p class="prof-addr-line">
              <b>{{ address.defaultAddress.name }}</b>
              <span>{{ maskPhone(address.defaultAddress.phone) }}</span>
              <i>{{ address.defaultAddress.isDefault ? '默认' : address.defaultAddress.tag }}</i>
            </p>
            <p class="prof-addr-text">
              {{ address.defaultAddress.region }} {{ address.defaultAddress.detail }}
            </p>
          </div>
          <div v-else>
            <p class="prof-addr-line"><b>还没有收货地址</b></p>
            <p class="prof-addr-text">点这里添加一个，下单时自动带出来</p>
          </div>
        </button>
      </section>

      <!-- 常用功能 -->
      <section class="prof-card">
        <header class="prof-card-head">
          <h2>常用功能</h2>
        </header>

        <ul class="prof-services">
          <li v-for="service in SERVICES" :key="service.key">
            <button type="button" @click="todo(service.label)">
              <span class="prof-service-ico">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  v-html="service.icon"
                />
              </span>
              <span class="prof-service-text">
                <b>{{ service.label }}</b>
                <small>{{ service.desc }}</small>
              </span>
              <svg
                class="prof-service-go"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="ICONS.chevron"
              />
            </button>
          </li>
        </ul>
      </section>

      <!-- 退出登录 -->
      <section class="prof-card prof-quit">
        <button
          class="prof-quit-btn"
          :class="{ 'is-armed': quitArmed }"
          type="button"
          @click="onQuit"
        >
          {{ quitArmed ? '再点一次确认退出' : '退出登录' }}
        </button>
        <p class="prof-quit-note">退出后购物车与消息仍留在本机，重新登录即可继续</p>
      </section>
    </div>
  </SubPage>

  <AppToast />
</template>
