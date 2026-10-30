<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SubPage from '@/components/SubPage.vue'
import AppToast from '@/components/AppToast.vue'
import ProductCard from '@/views/home/ProductCard.vue'
import { CATEGORIES, findProduct, type Category, type Product } from '@/views/home/catalog'
import { loadCategory, loadProduct } from '@/views/home/apiCatalog'
import { useShopActions } from '@/views/home/useShopActions'
import { MAX_QTY, useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { money } from '@/utils/format'
/* 商品卡样式在 home.css，详情页的「同类推荐」也要用 */
import '@/views/home/home.css'
import './item.css'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const pending = useCheckoutStore()
const { pickMore, pickLess, toggleCart, buyNow, goCheckout } = useShopActions()

const ICONS = {
  home: '<path d="M4 10.6 12 4l8 6.6V20h-5.4v-5.6H9.4V20H4Z"/>',
  check: '<path d="M5 12.6 10 17.6 19 8.2"/>',
  bag: '<path d="M6.4 8.2h11.2l.9 11.1a1.7 1.7 0 0 1-1.7 1.8H7.2a1.7 1.7 0 0 1-1.7-1.8Z"/><path d="M9 8.2V6.5a3 3 0 0 1 6 0v1.7"/>',
}

/**
 * 商品详情：先用静态目录垫上（后端慢或没起时也有东西看），
 * 再拉 `/c/products/{code}` 替换。拿到商品后按它的 category 拉整类，
 * 用于面包屑和「同类推荐」。
 */
const code = computed(() => String(route.params.id ?? ''))
const product = ref<Product | undefined>(findProduct(code.value))
const category = ref<Category | undefined>(staticCategoryOf(product.value))

function staticCategoryOf(p?: Product): Category | undefined {
  if (!p) return undefined
  if (p.category) return CATEGORIES.find((cat) => cat.id === p.category)
  return CATEGORIES.find((cat) => cat.items.some((item) => item.id === p.id))
}

async function loadDetail(id: string) {
  product.value = findProduct(id)
  category.value = staticCategoryOf(product.value)

  const loaded = await loadProduct(id)
  // 请求期间可能已经跳到别的商品，旧结果直接丢弃
  if (id !== code.value) return
  if (loaded) {
    product.value = loaded
    category.value = staticCategoryOf(loaded)
  }

  const catCode = product.value?.category
  if (!catCode) return
  const cat = await loadCategory(catCode)
  if (cat && id === code.value) category.value = cat
}

watch(code, (id) => void loadDetail(id), { immediate: true })

/** 生鲜 / 药品讲营养，其余讲参数 */
const NUTRITION_CATS = ['fruit', 'veg', 'meat', 'medicine']
const factsTitle = computed(() =>
  category.value && NUTRITION_CATS.includes(category.value.id) ? '营养成分' : '关键参数',
)

/** 商品所属细分 —— 面包屑里补一层，方便退回「这一类」的列表 */
const sub = computed(() => {
  const p = product.value
  if (!p) return undefined
  return category.value?.subs.find((item) => item.id === p.sub)
})

/** 同一细分下的其它商品，没有就退回整类，保证「同类推荐」不空 */
const siblings = computed(() => {
  const all = category.value?.items ?? []
  const sameSub = all.filter(
    (item) => item.id !== product.value?.id && item.sub === product.value?.sub,
  )
  const rest = all.filter((item) => item.id !== product.value?.id && item.sub !== product.value?.sub)
  return [...sameSub, ...rest].slice(0, 4)
})

/** 这一单里已选几件 —— 与商品卡上的数字同源 */
const selectedQty = computed(
  () => pending.lines.find((line) => line.id === product.value?.id)?.qty ?? 0,
)
const inCart = computed(() => !!cart.lines.find((line) => line.id === product.value?.id))

const saved = computed(() => {
  const p = product.value
  return p ? Math.max(0, p.list - p.price) : 0
})

function inc() {
  if (!product.value || selectedQty.value >= MAX_QTY) return
  pickMore(product.value, 1)
}

function dec() {
  if (!product.value) return
  pickLess(product.value)
}

watch(
  product,
  (value) => {
    document.title = value ? `${value.name} · OWL 商城` : '商品未找到 · OWL 商城'
  },
  { immediate: true },
)
</script>

<template>
  <SubPage
    :title="product?.name ?? '商品详情'"
    :subtitle="product?.detail.slogan ?? ''"
    :show-footer="pending.count > 0"
  >
    <template v-if="product">
      <nav class="crumb" aria-label="面包屑">
        <RouterLink :to="{ name: 'home' }" class="crumb-home">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.home"
          />
          首页
        </RouterLink>
        <span class="crumb-sep">/</span>
        <RouterLink
          v-if="category"
          class="crumb-cur"
          :to="{ name: 'category', params: { catId: category.id } }"
        >
          {{ category.name }}
        </RouterLink>
        <template v-if="category && sub">
          <span class="crumb-sep">/</span>
          <RouterLink
            class="crumb-cur"
            :to="{ name: 'category', params: { catId: category.id }, query: { sub: sub.id } }"
          >
            {{ sub.name }}
          </RouterLink>
        </template>
        <span class="crumb-sep">/</span>
        <span class="crumb-cur is-now">{{ product.name }}</span>
      </nav>

      <!-- 主图 + 购买区 -->
      <div class="hero">
        <div class="hero-shot">
          <span class="hero-tag">{{ product.tag }}</span>
          <img
            v-if="product.image"
            class="hero-img"
            :src="product.image"
            :alt="product.name"
            decoding="async"
          />
          <span v-else class="hero-emoji">{{ product.emoji }}</span>
        </div>

        <div class="hero-info">
          <p class="hero-slogan">{{ product.detail.slogan }}</p>
          <h2 class="hero-name">{{ product.name }}</h2>
          <p class="hero-spec">{{ product.spec }}</p>

          <div class="hero-price">
            <span class="hero-now"><small>¥</small>{{ money(product.price) }}</span>
            <del class="hero-was">¥{{ money(product.list) }}</del>
            <span v-if="saved > 0" class="hero-save">省 ¥{{ money(saved) }}</span>
          </div>

          <ul class="hero-facts">
            <li v-for="(value, key) in product.detail.facts" :key="key">
              <span>{{ key }}</span>
              <b>{{ value }}</b>
            </li>
          </ul>

          <div class="hero-act">
            <div class="hero-step" role="group" aria-label="本单已选数量">
              <button type="button" aria-label="减少数量" :disabled="selectedQty <= 0" @click="dec">
                −
              </button>
              <span>{{ selectedQty }}</span>
              <button type="button" aria-label="增加数量" :disabled="selectedQty >= MAX_QTY" @click="inc">
                +
              </button>
            </div>

            <button
              class="hero-btn hero-btn--cart"
              :class="{ 'is-added': inCart }"
              type="button"
              @click="toggleCart(product)"
            >
              <svg
                v-if="inCart"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="ICONS.check"
              />
              <svg
                v-else
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
                v-html="ICONS.bag"
              />
              {{ inCart ? '已加入购物车' : '加入购物车' }}
            </button>

            <button class="hero-btn hero-btn--buy" type="button" @click="buyNow(product)">
              立即购买
            </button>
          </div>

          <p class="hero-tip">
            加入购物车只放进「购物车」，不影响这一单；「+ −」改的是这一单的待购买清单。
          </p>

          <ul class="hero-serve">
            <li>满 ¥99 免运费</li>
            <li>预计 1 小时内送达</li>
            <li>7 天无理由退换</li>
          </ul>
        </div>
      </div>

      <!-- 商品介绍 -->
      <section class="sec">
        <h3 class="sec-head">
          商品介绍
          <span v-if="category">{{ category.name }}</span>
        </h3>
        <p class="sec-text">{{ product.detail.intro }}</p>
      </section>

      <!-- 营养成分 / 关键参数 -->
      <section class="sec">
        <h3 class="sec-head">
          {{ factsTitle }}
          <span>{{ Object.keys(product.detail.facts).length }} 项</span>
        </h3>
        <dl class="fact-grid">
          <div v-for="(value, key) in product.detail.facts" :key="key" class="fact">
            <dt>{{ key }}</dt>
            <dd>{{ value }}</dd>
          </div>
        </dl>
      </section>

      <!-- 选购贴士 -->
      <section class="sec">
        <h3 class="sec-head">选购贴士</h3>
        <ul class="tip-list">
          <li v-for="(tip, i) in product.detail.tips" :key="i">{{ tip }}</li>
        </ul>
      </section>

      <!-- 同类推荐 -->
      <section v-if="siblings.length" class="sec">
        <h3 class="sec-head">
          同类推荐
          <RouterLink
            v-if="category"
            class="sec-more"
            :to="{ name: 'category', params: { catId: category.id } }"
          >
            看全部 {{ category.items.length }} 件
          </RouterLink>
        </h3>
        <div class="product-grid">
          <ProductCard
            v-for="p in siblings"
            :key="p.id"
            :product="p"
            @add="pickMore"
            @dec="pickLess"
            @cart="toggleCart"
            @buy="buyNow"
          />
        </div>
      </section>
    </template>

    <div v-else class="miss">
      <p>这件商品已经不在了</p>
      <RouterLink class="miss-go" :to="{ name: 'home' }">回首页逛逛</RouterLink>
    </div>

    <template #footer>
      <div class="bar">
        <div class="bar-info">
          <span class="bar-count">已选 <b>{{ pending.count }}</b> 件</span>
          <span class="bar-amount"><small>¥</small>{{ money(pending.amount) }}</span>
        </div>
        <button class="bar-go" type="button" @click="goCheckout">去结算</button>
      </div>
    </template>
  </SubPage>

  <AppToast />
</template>
