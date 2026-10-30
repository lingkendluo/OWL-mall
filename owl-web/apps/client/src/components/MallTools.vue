<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { useMessageStore } from '@/stores/messages'
import './mall-tools.css'

const cart = useCartStore()
/** 待付款 = 结算清单（点「立即购买」或「去结算」才进来），角标按商品种类数 */
const pending = useCheckoutStore()
const messages = useMessageStore()
const route = useRoute()

/**
 * 已经在子页里时，再点这几个入口用 replace 换页而不是 push：
 * 否则 首页→购物车→个人中心→消息 会叠三层历史，浏览器要退三次才回首页。
 * 从首页点进去仍然 push，这样「←」和浏览器后退都能一步回首页。
 */
const replaceMode = computed(() => route.name !== 'home')

const ICONS = {
  user: '<path d="M12 11.8a3.6 3.6 0 1 0 0-7.2 3.6 3.6 0 0 0 0 7.2Z"/><path d="M4.8 20.2c.6-3.6 3.6-5.6 7.2-5.6s6.6 2 7.2 5.6"/>',
  cart: '<path d="M3.2 4.4h2.2l2.3 10.6a2 2 0 0 0 2 1.6h7.6a2 2 0 0 0 1.95-1.55L20.4 8H6"/><path d="M10 20.2a1.4 1.4 0 1 0 0-2.8 1.4 1.4 0 0 0 0 2.8Z"/><path d="M17.4 20.2a1.4 1.4 0 1 0 0-2.8 1.4 1.4 0 0 0 0 2.8Z"/>',
  wallet: '<path d="M4.4 7.8h15.2v9.6a2.2 2.2 0 0 1-2.2 2.2H6.6a2.2 2.2 0 0 1-2.2-2.2Z"/><path d="M4.4 7.8V6.6a2 2 0 0 1 2-2h8.2"/><path d="M15.4 12.1h4.2v3.2h-4.2a1.6 1.6 0 0 1 0-3.2Z"/>',
  msg: '<path d="M20.5 12.2c0 4.1-3.8 7.4-8.5 7.4-1 0-2-.15-2.9-.42L4 21l1.4-3.6C4.2 16.2 3.5 14.3 3.5 12.2 3.5 8.1 7.3 4.8 12 4.8s8.5 3.3 8.5 7.4Z"/>',
}

const LINKS = [
  { name: 'profile', label: '个人中心', icon: ICONS.user },
  { name: 'cart', label: '购物车', icon: ICONS.cart },
  { name: 'checkout', label: '待付款', icon: ICONS.wallet },
  { name: 'messages', label: '消息', icon: ICONS.msg },
] as const
</script>

<template>
  <nav class="mall-tools">
    <RouterLink
      v-for="link in LINKS"
      :key="link.name"
      :to="{ name: link.name }"
      :replace="replaceMode"
      class="mall-tool"
      :class="{ 'is-active': route.name === link.name }"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.7"
        stroke-linecap="round"
        stroke-linejoin="round"
        v-html="link.icon"
      />
      <span>{{ link.label }}</span>
      <em v-if="link.name === 'cart' && cart.count" class="badge">{{ cart.count }}</em>
      <em
        v-if="link.name === 'checkout' && pending.lines.length"
        class="badge is-alert"
        :aria-label="`待付款 ${pending.lines.length} 种商品`"
        >{{ pending.lines.length }}</em
      >
      <i v-if="link.name === 'messages' && messages.unread" class="ping" />
    </RouterLink>
  </nav>
</template>
