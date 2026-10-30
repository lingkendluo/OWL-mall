/**
 * 后端商品数据 → 前端视图类型的适配层。
 *
 * 设计原则：**后端优先，静态兜底**。
 *   · 接口通：首页分类、细分、商品、搜索全部来自 `/api/v1/c/**`，
 *     管理端下架/上新后刷新页面就能看到变化；
 *   · 接口挂（后端或 Redis 没起）：返回 null，调用方继续用 catalog.ts 里的静态目录，
 *     页面照常能看，不会白屏。
 *
 * 之所以单独一层，是为了让 HomeView / CategoryView / ProductDetailView 的模板
 * 完全不用改 —— 它们仍然吃 Category / Product 这套结构。
 */
import { productApi, type ApiCategory, type ApiProduct } from '@/services/productApi'
import {
  ALL_PRODUCTS,
  findCategory,
  findProduct,
  type Category,
  type Product,
  type ProductDetail,
} from './catalog'

/** 后端 detail 可能为 null，补一份空壳，避免详情页读 slogan 时炸掉 */
const EMPTY_DETAIL: ProductDetail = { slogan: '', intro: '', facts: {}, tips: [] }

function toProduct(raw: ApiProduct): Product {
  const detail = raw.detail
  return {
    id: raw.id,
    name: raw.name,
    spec: raw.spec ?? '',
    price: Number(raw.price ?? 0),
    list: Number(raw.list ?? raw.price ?? 0),
    tag: raw.tag ?? '',
    emoji: raw.emoji ?? '',
    image: raw.image ?? '',
    sub: raw.sub ?? '',
    category: raw.category ?? '',
    detail: detail
      ? {
          slogan: detail.slogan ?? '',
          intro: detail.intro ?? '',
          facts: detail.facts ?? {},
          tips: detail.tips ?? [],
        }
      : { ...EMPTY_DETAIL },
  }
}

function toCategory(raw: ApiCategory): Category {
  return {
    id: raw.id,
    name: raw.name,
    hint: raw.hint ?? '',
    glyph: raw.glyph ?? '',
    subs: (raw.subs ?? []).map((sub) => ({
      id: sub.id,
      name: sub.name,
      count: sub.count ?? 0,
    })),
    items: (raw.items ?? []).map(toProduct),
  }
}

/** 首页聚合数据；拿不到返回 null，让调用方留在静态目录上 */
export async function loadHome(): Promise<{ categories: Category[]; featured: Product[] } | null> {
  try {
    const home = await productApi.home()
    if (!home?.categories?.length) return null
    return {
      categories: home.categories.map(toCategory),
      // featured 里已经带 detail，转出来就能直接进详情页
      featured: (home.featured ?? []).map(toProduct),
    }
  } catch {
    return null
  }
}

/** 单个大类的全部在售商品（不带 sub，细分筛选留在前端做，交互和原来一致） */
export async function loadCategory(code: string): Promise<Category | null> {
  try {
    const data = await productApi.category(code)
    if (!data) return null
    return toCategory(data)
  } catch {
    return null
  }
}

/** 商品详情；失败退回静态目录里的同名商品 */
export async function loadProduct(code: string): Promise<Product | null> {
  try {
    const data = await productApi.product(code)
    if (!data) return findProduct(code) ?? null
    return toProduct(data)
  } catch {
    return findProduct(code) ?? null
  }
}

/** 搜索走后端；失败时退回本地静态目录里扫一遍，保证空网也能搜 */
export async function searchProducts(keyword: string, size = 60): Promise<Product[]> {
  const kw = keyword.trim()
  if (!kw) return []
  try {
    const page = await productApi.search({ keyword: kw, size })
    return (page.list ?? []).map(toProduct)
  } catch {
    const lower = kw.toLowerCase()
    return ALL_PRODUCTS.filter(
      (item) =>
        item.name.toLowerCase().includes(lower) || item.spec.toLowerCase().includes(lower),
    ).slice(0, size)
  }
}

export { findCategory, findProduct }
