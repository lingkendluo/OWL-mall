<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import MallTools from '@/components/MallTools.vue'
import { useAssistantStore } from '@/stores/assistant'
import { useAddressStore } from '@/stores/address'
import { FREE_SHIPPING, useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { noticeApi, type PublicNotice } from '@shared/services/notices'
import { CATEGORIES, FEATURED, HOME_CATEGORY, type Category, type Product } from './catalog'
import { loadCategory, loadHome, searchProducts } from './apiCatalog'
import { money, round2 } from '@/utils/format'
import ProductCard from './ProductCard.vue'
import './home.css'

const ICONS = {
  owl: '<path d="M7 11.4 6.6 5.6l4.7 3.6"/><path d="M25 11.4l.4-5.8-4.7 3.6"/><path d="M16 6.6c5.6 0 9.4 4.3 9.4 9.7s-4 10.9-9.4 10.9-9.4-5.5-9.4-10.9S10.4 6.6 16 6.6Z"/><path d="M11.9 14.4a3.5 3.5 0 1 0 0 0Z"/><path d="M20.1 14.4a3.5 3.5 0 1 0 0 0Z"/><path d="M16 17.6l-1.4 2.2h2.8L16 17.6Z"/>',
  search:
    '<path d="M11 17.5a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13Z"/><path d="M15.8 15.8 20.5 20.5"/>',
  pin: '<path d="M12 21c4.2-4 6.4-7 6.4-9.8A6.4 6.4 0 0 0 5.6 11.2C5.6 14 7.8 17 12 21Z"/><path d="M12 13.4a2.2 2.2 0 1 0 0-4.4 2.2 2.2 0 0 0 0 4.4Z"/>',
  chevron: '<path d="M8 10.5 12 14.5 16 10.5"/>',
  bag: '<path d="M6.4 8.2h11.2l.9 11.1a1.7 1.7 0 0 1-1.7 1.8H7.2a1.7 1.7 0 0 1-1.7-1.8Z"/><path d="M9 8.2V6.5a3 3 0 0 1 6 0v1.7"/>',
  arrow: '<path d="M5 12h13M13 6.5 18.5 12 13 17.5"/>',
}

const router = useRouter()
const cart = useCartStore()
const address = useAddressStore()
const pending = useCheckoutStore()
const assistant = useAssistantStore()

/*
 * 顶栏公告：唯一走真后端的地方（/c/notices 免鉴权，管理端发布后立刻能看见）。
 * 后端没起就静默收起来，不能因为一条公告挂掉整个首页。
 */
const notices = ref<PublicNotice[]>([])
const noticeIndex = ref(0)
const currentNotice = computed<PublicNotice | null>(() => notices.value[noticeIndex.value] ?? null)

function nextNotice() {
  if (notices.value.length < 2) return
  noticeIndex.value = (noticeIndex.value + 1) % notices.value.length
}

onMounted(async () => {
  try {
    notices.value = await noticeApi.list(3)
  } catch {
    notices.value = []
  }
})

/*
 * 商品数据：后端优先，静态兜底。
 * 初值先放静态目录，`/c/home` 回来后再整体替换 —— 这样即使后端没起、
 * 或者接口慢，首屏也有东西看，不会白屏。
 */
const categories = ref<Category[]>(CATEGORIES)
const featured = ref<Product[]>(FEATURED)
const homeCategory = ref<Category>(HOME_CATEGORY)

/** 左栏顺序：推荐在最前，后面跟全部大类 */
const rail = computed<Category[]>(() => [homeCategory.value, ...categories.value])

/** 按大类缓存的商品，切过一次之后来回切不再请求 */
const itemsByCat = ref<Record<string, Product[]>>({})

/** 大类的商品还没拉到时的占位，避免把静态数据当成后端数据 */
async function ensureItems(id: string) {
  if (id === homeCategory.value.id) return
  if (itemsByCat.value[id]) return
  const loaded = await loadCategory(id)
  if (loaded) {
    itemsByCat.value = { ...itemsByCat.value, [id]: loaded.items }
  }
}

onMounted(async () => {
  const home = await loadHome()
  if (!home) return
  categories.value = home.categories
  // 推荐位对不上就退回静态的 12 件，总比空着强
  featured.value = home.featured.length ? home.featured : FEATURED
  homeCategory.value = { ...HOME_CATEGORY, items: featured.value }
})

/*
 * 底栏只算「这一单」：待购买清单里选中商品的价格总和，跟购物车完全无关。
 * 运费放到结算页算，这里只给一句免运费的进度提示。
 */
const pendingFreeShip = computed(() => pending.amount >= FREE_SHIPPING)
const pendingToFree = computed(() => round2(Math.max(0, FREE_SHIPPING - pending.amount)))

/** 推荐页分类面板底部那句「N 个大类 · M 个细分」 */
const totalSubs = computed(() => categories.value.reduce((sum, cat) => sum + cat.subs.length, 0))

const activeId = ref('all')
const keyword = ref('')
const toastText = ref('')
const mainEl = ref<HTMLElement | null>(null)

let toastTimer: number | undefined

const searching = computed(() => keyword.value.trim().length > 0)

const current = computed<Category>(() => {
  const base = rail.value.find((cat) => cat.id === activeId.value) ?? homeCategory.value
  // 这一类的商品已经拉回来了就换上，没拉到就先用（可能是空的）静态内容
  const loaded = itemsByCat.value[base.id]
  return loaded ? { ...base, items: loaded } : base
})

/**
 * 搜索改成走后端 `/c/products?keyword=`：
 * 原来是在静态目录里遍历，接后端后商品会变，本地扫描必然漏。
 * 220ms 防抖，避免一个字一个请求。
 */
const searchHits = ref<Product[]>([])
let searchTimer: number | undefined

watch(
  () => keyword.value.trim(),
  (kw) => {
    window.clearTimeout(searchTimer)
    if (!kw) {
      searchHits.value = []
      return
    }
    searchTimer = window.setTimeout(async () => {
      searchHits.value = await searchProducts(kw)
    }, 220)
  },
)

function toast(message: string) {
  toastText.value = message
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toastText.value = ''
  }, 2200)
}

/**
 * 桌面端右栏是独立滚动容器（.mall-main），移动端仍是整页滚动。
 * 切换品类时要把内容区滚回顶部，两种模式各走各的 API。
 */
function innerScroller(): HTMLElement | null {
  const el = mainEl.value
  if (!el) return null
  return getComputedStyle(el).overflowY === 'auto' ? el : null
}

function scrollMainTo(top: number) {
  const box = innerScroller()
  if (box) box.scrollTo({ top, behavior: 'smooth' })
  else window.scrollTo({ top, behavior: 'smooth' })
}

function pickCat(id: string) {
  activeId.value = id
  keyword.value = ''
  scrollMainTo(0)
  // 切到大类就顺手把这一类的商品拉回来，下次再切回来直接命中缓存
  void ensureItems(id)
}

/**
 * 细分小类点击：一律进「这一类的商品总览」。
 * 跳分类页并带上 ?sub=，页头会变成「零食 · 果冻布丁 · 8 件」，
 * 筛选条上对应的细分保持选中，列表只留这一类 —— 哪怕这一类只有 1 件，
 * 也是先看到总览，再从卡片点进详情，不替用户做决定。
 */
function subItems(catId: string, subId: string): Product[] {
  const cat = categories.value.find((item) => item.id === catId)
  if (!cat) return []
  // 商品可能是按需拉的；没拉过就用细分自带的件数兜底，别再回静态目录里找
  const loaded = itemsByCat.value[catId]
  return loaded ? loaded.filter((item) => item.sub === subId) : []
}

function openSub(catId: string, subId: string) {
  void router.push({ name: 'category', params: { catId }, query: { sub: subId } })
}

/** 鼠标悬停提示：说清楚点进去会看到几件 */
function subHint(catId: string, subId: string, name: string): string {
  const cat = categories.value.find((item) => item.id === catId)
  const fromSubs = cat?.subs.find((sub) => sub.id === subId)?.count ?? 0
  const count = fromSubs || subItems(catId, subId).length
  if (count === 0) return `「${name}」还在补货`
  return `查看「${name}」全部 ${count} 件`
}

/* ------------------------------ 配送地址 ------------------------------ */

/** 头部只放得下一行，取「市 · 区+街道」再把门牌截短 */
const addressBrief = computed(() => {
  const hit = address.defaultAddress
  if (!hit) return '选择收货地址'
  const parts = hit.region.split(/\s+/).filter(Boolean)
  const city = parts[1] ?? parts[0] ?? ''
  const rest = parts.slice(2).join('')
  const brief = `${city}${rest ? ' · ' + rest : ''}${hit.detail}`
  return brief.length > 22 ? `${brief.slice(0, 21)}…` : brief
})

const addressTitle = computed(() => {
  const hit = address.defaultAddress
  if (!hit) return '还没有收货地址，点击添加'
  return `${hit.name} ${hit.phone}\n${hit.region} ${hit.detail}`
})

function openAddresses() {
  void router.push({ name: 'addresses' })
}

/** 商品卡只负责选数量，存哪儿由这两个动作决定 */
function snapshot(product: Product) {
  return {
    id: product.id,
    name: product.name,
    spec: product.spec,
    price: product.price,
    list: product.list,
    emoji: product.emoji,
    image: product.image,
  }
}

/**
 * 卡片上的「+」：只往「当前这一单」里加，跟购物车没有关系。
 * 所以底栏合计 = 这一单所有选中商品之和，点别的商品就是累加。
 */
function pickMore(product: Product, qty: number) {
  const before = pending.lines.find((line) => line.id === product.id)?.qty ?? 0
  pending.add(snapshot(product), qty)
  if (before === 0) toast(`已选中 · ${product.name}`)
}

/** 卡片上的「−」：够 2 件就减一，只剩 1 件时减到 0（=从这一单里去掉） */
function pickLess(product: Product) {
  const line = pending.lines.find((l) => l.id === product.id)
  if (!line) return
  if (line.qty > 1) pending.decrease(product.id)
  else pending.remove(product.id)
}

/**
 * 「加入购物车」按钮是个开关：不在购物车就加一件，已经在购物车里再点一次就移出。
 * 只动购物车，不改这一单的选中数量，也不进底栏合计。
 */
function toggleCart(product: Product) {
  const hit = cart.lines.find((line) => line.id === product.id)
  if (hit) {
    cart.remove(product.id)
    toast(`已从购物车移出 · ${product.name}`)
    return
  }
  cart.add(snapshot(product), 1)
  toast(`已加入购物车 · ${product.name}`)
}

/** 「立即购买」：这一单里已经有它就按选中数量直接去结算，没有就先放 1 件 */
function buyNow(product: Product) {
  if (!pending.lines.some((line) => line.id === product.id)) pending.add(snapshot(product), 1)
  void router.push({ name: 'checkout' })
}

function goCheckout() {
  router.push({ name: 'checkout' })
}

watch(searching, (on) => {
  if (on) scrollMainTo(0)
})

onBeforeUnmount(() => window.clearTimeout(toastTimer))
</script>

<template>
  <div class="mall">
    <!-- ================= 顶部栏 ================= -->
    <header class="mall-top">
      <div class="mall-top-inner">
        <button
          class="assistant"
          type="button"
          title="打开 OWL 购物分析助手"
          @click="assistant.openPanel()"
        >
          <span class="assistant-logo">
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
          <span class="assistant-text">OWL购物分析助手</span>
        </button>

        <div class="mall-search">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.search"
          />
          <input v-model="keyword" type="search" placeholder="搜索商品或品类，如「大米」「耳机」" />
          <button v-if="keyword" class="search-clear" type="button" @click="keyword = ''">
            清除
          </button>
        </div>

        <MallTools />
      </div>
    </header>

    <!-- ================= 配送条 ================= -->
    <div class="mall-ship">
      <div class="mall-ship-inner">
        <button class="addr" type="button" :title="addressTitle" @click="openAddresses">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.pin"
          />
          <span class="addr-label">配送至</span>
          <b>{{ addressBrief }}</b>
          <svg
            class="addr-caret"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="ICONS.chevron"
          />
        </button>

        <div class="ship-tags">
          <span class="live"><i />预计 1 小时内送达</span>
          <span>满 ¥99 免运费</span>
          <span>08:00 - 22:00 营业中</span>
        </div>
      </div>
    </div>

    <div class="mall-body">
      <!-- ================= 左侧大类 ================= -->
      <aside class="mall-rail">
        <p class="rail-head">商品分类</p>
        <button
          v-for="cat in rail"
          :key="cat.id"
          class="cat-pill"
          :class="{ 'is-active': activeId === cat.id }"
          :title="cat.hint"
          type="button"
          @click="pickCat(cat.id)"
        >
          <svg
            class="cat-ico"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
            v-html="cat.glyph"
          />
          <span class="cat-name">{{ cat.name }}</span>
        </button>
      </aside>

      <!-- ================= 主内容 ================= -->
      <main ref="mainEl" class="mall-main">
        <!-- 品牌条：红晕里的一行，让 OWL 商城一直露脸 -->
        <div class="owl-brand">
          <span class="owl-brand-mark">OWL</span>
          <b class="owl-brand-name">OWL 商城</b>
          <span class="owl-brand-desc">全品类好物 · 最快 1 小时送达</span>
        </div>

        <!-- 公告条：内容来自后台「公告管理」，没发布就不显示 -->
        <div v-if="currentNotice" class="notice-strip">
          <span class="notice-tag">{{ currentNotice.typeText }}</span>
          <p class="notice-text">
            <b>{{ currentNotice.title }}</b>
            <span>{{ currentNotice.body }}</span>
          </p>
          <span class="notice-time">{{ currentNotice.time }}</span>
          <button
            v-if="notices.length > 1"
            class="notice-more"
            type="button"
            :title="`还有 ${notices.length - 1} 条，点击切换`"
            @click="nextNotice"
          >
            {{ noticeIndex + 1 }}/{{ notices.length }}
          </button>
        </div>

        <!-- 搜索结果 -->
        <section v-if="searching" class="search-panel">
          <header class="panel-head">
            <h2>「{{ keyword.trim() }}」的搜索结果</h2>
            <span>共 {{ searchHits.length }} 件</span>
          </header>
          <div v-if="searchHits.length" class="product-grid">
            <ProductCard
              v-for="p in searchHits"
              :key="p.id"
              :product="p"
              @add="pickMore"
              @dec="pickLess"
              @cart="toggleCart"
              @buy="buyNow"
            />
          </div>
          <p v-else class="empty">没有找到相关商品，换个关键词试试</p>
          <p v-if="searchHits.length" class="search-tip">
            没找到想要的？<button type="button" @click="keyword = ''">
              返回{{ current.name }}
            </button>
          </p>
        </section>

        <!-- 视图切换：推荐页 / 分类页 -->
        <div v-else :key="activeId" class="view">
          <!-- ========== 推荐页 ========== -->
          <template v-if="activeId === 'all'">
            <!-- 全部分类速览：一行一个大类，右边是该类的细分文字并排 -->
            <section class="cat-board">
              <header class="cat-board-head">
                <h2>全部分类</h2>
                <span class="cat-board-count">
                  {{ categories.length }} 个大类 · {{ totalSubs }} 个细分
                </span>
              </header>

              <div class="cat-lines">
                <div v-for="cat in categories" :key="cat.id" class="cat-line">
                  <button class="cat-key" type="button" :title="cat.hint" @click="pickCat(cat.id)">
                    {{ cat.name }}
                  </button>
                  <div class="cat-subs">
                    <button
                      v-for="sub in cat.subs"
                      :key="sub.id"
                      class="cat-sub"
                      :class="{ 'is-off': !sub.count }"
                      type="button"
                      :disabled="!sub.count"
                      :title="subHint(cat.id, sub.id, sub.name)"
                      @click="openSub(cat.id, sub.id)"
                    >
                      {{ sub.name }}
                    </button>
                  </div>
                </div>
              </div>
            </section>

            <section id="featured" class="floor">
              <header class="floor-head">
                <div class="floor-title">
                  <h2>为你推荐</h2>
                  <span>跨品类精选 · {{ featured.length }} 件</span>
                </div>
              </header>
              <div class="product-grid">
                <ProductCard
                  v-for="p in featured"
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

          <!-- ========== 分类页 ========== -->
          <template v-else>
            <section class="subs">
              <header class="subs-head">
                <h2>细分品类</h2>
                <RouterLink
                  class="subs-all"
                  :to="{ name: 'category', params: { catId: current.id } }"
                >
                  查看全部
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    v-html="ICONS.arrow"
                  />
                </RouterLink>
              </header>
              <div class="subs-grid">
                <button
                  v-for="(sub, i) in current.subs"
                  :key="sub.id"
                  class="sub-card"
                  :class="{ 'is-off': !sub.count }"
                  type="button"
                  :disabled="!sub.count"
                  :title="subHint(current.id, sub.id, sub.name)"
                  @click="openSub(current.id, sub.id)"
                >
                  <span class="sub-no">
                    {{ String(i + 1).padStart(2, '0') }} ·
                    {{ sub.count ? `${sub.count} 件` : '补货中' }}
                  </span>
                  <span class="sub-name">{{ sub.name }}</span>
                  <svg
                    class="sub-go"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    v-html="ICONS.arrow"
                  />
                </button>
              </div>
            </section>

            <section class="floor">
              <header class="floor-head">
                <div class="floor-title">
                  <h2>本类热销</h2>
                  <span>{{ current.name }} · {{ current.items.length }} 件</span>
                </div>
              </header>
              <div class="product-grid">
                <ProductCard
                  v-for="p in current.items"
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
        </div>
      </main>
    </div>

    <!--
      底部条：合计 = 「这一单当前选中的商品」价格总和，与购物车无关。
      卡片上的「−/＋」改的是待购买清单，所以这里实时变；
      购物车里的商品不参与这里的任何数字，只有购物车页面才看得到。
    -->
    <div v-if="pending.count" class="cartbar-wrap">
      <div class="cartbar">
        <div class="cartbar-cart">
          <!-- 左侧购物袋图标同时也是「待付款」入口：点它和点「去结算」效果一致 -->
          <button
            class="cartbar-ico"
            type="button"
            title="查看待付款清单"
            :aria-label="`查看待付款清单，共 ${pending.count} 件`"
            @click="goCheckout"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
              v-html="ICONS.bag"
            />
          </button>
          <span class="cartbar-info">
            <b>已选 {{ pending.count }} 件</b>
            <small>
              {{ pendingFreeShip ? '已免运费' : `再买 ¥${money(pendingToFree)} 免运费` }}
            </small>
          </span>
          <span class="cartbar-amount"><small>¥</small>{{ money(pending.amount) }}</span>
        </div>

        <button class="cartbar-go" type="button" @click="goCheckout">去结算</button>
      </div>
    </div>

    <div class="mall-toast" :class="{ show: !!toastText }">
      <i />
      {{ toastText }}
    </div>
  </div>
</template>
