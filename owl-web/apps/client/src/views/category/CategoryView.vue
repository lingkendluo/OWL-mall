<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SubPage from '@/components/SubPage.vue'
import AppToast from '@/components/AppToast.vue'
import ProductCard from '@/views/home/ProductCard.vue'
import { findCategory, type Category } from '@/views/home/catalog'
import { loadCategory } from '@/views/home/apiCatalog'
import { useShopActions } from '@/views/home/useShopActions'
/* 商品卡与栅格样式收在 home.css 里，这里复用同一套，保证卡片长相一致 */
import '@/views/home/home.css'
import './category.css'

const route = useRoute()
const router = useRouter()
const { pickMore, pickLess, toggleCart, buyNow } = useShopActions()

const ICONS = {
  home: '<path d="M4 10.6 12 4l8 6.6V20h-5.4v-5.6H9.4V20H4Z"/>',
}

const catId = computed(() => String(route.params.catId ?? ''))

/**
 * 分类数据：先用静态目录垫上（切换时立刻有内容），
 * 后端 `/c/categories/{code}` 回来再整体替换；接口失败就保持静态内容。
 */
const category = ref<Category | undefined>(findCategory(catId.value))

async function loadCat(code: string) {
  category.value = findCategory(code)
  const loaded = await loadCategory(code)
  // 请求期间用户可能已经切到别的大类了，别把旧数据盖上去
  if (loaded && code === catId.value) category.value = loaded
}

watch(catId, (code) => void loadCat(code), { immediate: true })

const subId = computed(() => {
  const raw = route.query.sub
  return typeof raw === 'string' ? raw : ''
})

const currentSub = computed(() => category.value?.subs.find((s) => s.id === subId.value))

/** 筛选条只列有货的细分 —— 一半选项是空的会让整排筛选变成噪音 */
const stockedSubs = computed(() => (category.value?.subs ?? []).filter((s) => s.count > 0))

const matched = computed(() => {
  const items = category.value?.items ?? []
  if (!subId.value) return items
  return items.filter((item) => item.sub === subId.value)
})

/** 细分下暂时没货时退回整类，别让用户看到空页面 */
const fallback = computed(() => !!subId.value && matched.value.length === 0)
const shown = computed(() => (fallback.value ? (category.value?.items ?? []) : matched.value))

/**
 * 只筛出 1~2 件细分时，页面下半屏会空着。
 * 补一栏「本类的其它商品」把位置填上，标题写清楚是别的细分，避免误认成同一类。
 */
const otherItems = computed(() => {
  if (!subId.value) return []
  return (category.value?.items ?? []).filter((item) => item.sub !== subId.value).slice(0, 4)
})

const subtitle = computed(() => {
  if (!category.value) return ''
  const total = category.value.items.length
  return currentSub.value
    ? `${currentSub.value.name} · ${matched.value.length || total} 件`
    : `${category.value.hint} · ${total} 件`
})

function pickSub(id: string) {
  if (id === subId.value) return
  void router.replace({
    name: 'category',
    params: { catId: catId.value },
    query: id ? { sub: id } : {},
  })
}

watch(
  category,
  (value) => {
    document.title = value ? `${value.name} · OWL 商城` : '分类未找到 · OWL 商城'
  },
  { immediate: true },
)
</script>

<template>
  <SubPage :title="category?.name ?? '分类'" :subtitle="subtitle">
    <nav v-if="category" class="crumb" aria-label="面包屑">
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
      <RouterLink :to="{ name: 'category', params: { catId: category.id } }" class="crumb-cur">
        {{ category.name }}
      </RouterLink>
      <template v-if="currentSub">
        <span class="crumb-sep">/</span>
        <span class="crumb-cur is-now">{{ currentSub.name }}</span>
      </template>
    </nav>

    <template v-if="category">
      <!-- 细分筛选条：全部 + 本类 8 个细分 -->
      <div class="subbar">
        <button class="subchip" :class="{ 'is-on': !subId }" type="button" @click="pickSub('')">
          全部
          <b>{{ category.items.length }}</b>
        </button>
        <button
          v-for="sub in stockedSubs"
          :key="sub.id"
          class="subchip"
          :class="{ 'is-on': subId === sub.id }"
          type="button"
          @click="pickSub(sub.id)"
        >
          {{ sub.name }}
          <b>{{ sub.count }}</b>
        </button>
      </div>

      <p v-if="fallback" class="notice">
        「{{ currentSub?.name }}」还在补货，先看看{{ category.name }}的热销款
      </p>

      <div class="product-grid">
        <ProductCard
          v-for="p in shown"
          :key="p.id"
          :product="p"
          @add="pickMore"
          @dec="pickLess"
          @cart="toggleCart"
          @buy="buyNow"
        />
      </div>

      <section v-if="otherItems.length" class="floor">
        <header class="floor-head">
          <div class="floor-title">
            <h2>{{ category.name }}的其它商品</h2>
            <span>不在「{{ currentSub?.name }}」里，换换口味</span>
          </div>
          <RouterLink class="subs-all" :to="{ name: 'category', params: { catId: category.id } }">
            查看全部 {{ category.items.length }} 件
          </RouterLink>
        </header>
        <div class="product-grid">
          <ProductCard
            v-for="p in otherItems"
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
      <p>没有找到这个分类</p>
      <RouterLink class="miss-go" :to="{ name: 'home' }">回首页逛逛</RouterLink>
    </div>
  </SubPage>

  <AppToast />
</template>
