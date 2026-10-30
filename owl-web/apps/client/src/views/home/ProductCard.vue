<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import type { Product } from './catalog'
import { MAX_QTY, useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { money } from '@/utils/format'

const props = defineProps<{ product: Product }>()

const emit = defineEmits<{
  /** 「+」：在这一单里多选一件 */
  add: [product: Product, qty: number]
  /** 「−」：在这一单里少选一件，减到 0 即取消选中 */
  dec: [product: Product]
  /** 「加入购物车」：独立清单，与上面两个互不影响 */
  cart: [product: Product]
  buy: [product: Product]
}>()

/** 两份互不相干的清单：selected = 这一单要买的，cart = 长期存放的 */
const selected = useCheckoutStore()
const cart = useCartStore()

/**
 * 卡片上的数字 = 这件商品「这一单选中了几件」，0 表示没选。
 * 只来自待购买清单，和购物车没有关系：加购不会让这里变数，
 * 这里加减也不会往购物车里放东西。
 */
const selectedQty = computed(
  () => selected.lines.find((line) => line.id === props.product.id)?.qty ?? 0,
)

/** 购物车里有没有这件 —— 只决定按钮长相，不参与上面那个数字和底栏合计 */
const inCart = computed(() => cart.lines.some((line) => line.id === props.product.id))

/** 刚点完的那一下做个缩放反馈 */
const justAdded = ref(false)
let addedTimer = 0

function flash() {
  justAdded.value = true
  window.clearTimeout(addedTimer)
  addedTimer = window.setTimeout(() => {
    justAdded.value = false
  }, 900)
}

function inc() {
  if (selectedQty.value >= MAX_QTY) return
  emit('add', props.product, 1)
}

/** 减：1 件也能减，减到 0 就是把这件从这一单里去掉 */
function dec() {
  if (selectedQty.value <= 0) return
  emit('dec', props.product)
}

/** 加入购物车 / 再次点击移出购物车 —— 由父组件决定是加还是删 */
function toggleCart() {
  emit('cart', props.product)
  flash()
}

onBeforeUnmount(() => window.clearTimeout(addedTimer))
</script>

<template>
  <article class="pcard">
    <RouterLink
      class="pcard-media"
      :to="{ name: 'item', params: { id: product.id } }"
      :aria-label="`查看 ${product.name} 详情`"
    >
      <span class="pcard-tag">{{ product.tag }}</span>
      <img
        v-if="product.image"
        class="pcard-photo"
        :src="product.image"
        :alt="product.name"
        loading="lazy"
        decoding="async"
      />
      <span v-else class="pcard-emoji">{{ product.emoji }}</span>
    </RouterLink>

    <h3 class="pcard-name" :title="props.product.name">
      <RouterLink :to="{ name: 'item', params: { id: product.id } }">{{ product.name }}</RouterLink>
    </h3>
    <p class="pcard-spec">{{ product.spec }}</p>

    <div class="pcard-foot">
      <p class="pcard-price">
        <small>¥</small>{{ money(product.price)
        }}<del>¥{{ money(product.list) }}</del>
      </p>

      <div
        class="pcard-step"
        :class="{ 'is-off': selectedQty === 0 }"
        role="group"
        :aria-label="`${product.name} 本单已选数量`"
      >
        <button
          class="pcard-step-btn"
          type="button"
          aria-label="减少数量"
          :disabled="selectedQty <= 0"
          @click="dec"
        >
          −
        </button>
        <span class="pcard-step-num">{{ selectedQty }}</span>
        <button
          class="pcard-step-btn"
          type="button"
          aria-label="增加数量"
          :disabled="selectedQty >= MAX_QTY"
          @click="inc"
        >
          +
        </button>
      </div>
    </div>

    <div class="pcard-acts">
      <button
        class="pcard-act pcard-act--cart"
        :class="{ 'is-added': inCart, 'is-just-added': justAdded }"
        type="button"
        :aria-label="inCart ? `移出购物车 ${product.name}` : `加入购物车 ${product.name}`"
        :title="inCart ? '再次点击移出购物车' : ''"
        @click="toggleCart"
      >
        <svg
          v-if="inCart"
          class="pcard-act-check"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M5 12.6 10 17.6 19 8.2" />
        </svg>
        <svg
          v-if="inCart"
          class="pcard-act-x"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M6.4 6.4 17.6 17.6M17.6 6.4 6.4 17.6" />
        </svg>
        <template v-if="inCart">
          <span class="pcard-label-full">已加入购物车</span>
          <span class="pcard-label-short">已加入</span>
          <span class="pcard-label-out">移出购物车</span>
        </template>
        <template v-else>加入购物车</template>
      </button>
      <button
        class="pcard-act pcard-act--buy"
        type="button"
        @click="emit('buy', product)"
      >
        立即购买
      </button>
    </div>
  </article>
</template>
