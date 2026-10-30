/**
 * 用户端商品接口，对应 owl-server 的 CategoryController / ProductController：
 *   GET /c/home                 首页聚合（分类 + 推荐 + 包邮门槛）
 *   GET /c/categories           全部分类（含细分与件数）
 *   GET /c/categories/{code}    单个分类（items 已填充，可带 ?sub= 过滤）
 *   GET /c/products             列表 / 搜索
 *   GET /c/products/{code}      详情
 *
 * 这里只放「后端长什么样」，转成前端视图类型（Category/Product）的活交给
 * views/home/apiCatalog.ts，避免 services 反向依赖 views。
 */
import { http, withQuery } from '@shared/http'

export interface ApiDetail {
  slogan?: string
  intro?: string
  /** 关键参数 / 营养成分，键值都是给人看的字符串 */
  facts?: Record<string, string>
  tips?: string[]
}

export interface ApiProduct {
  id: string
  name: string
  spec?: string
  /** 售价，元 */
  price: number
  /** 划线价，元 */
  list?: number
  tag?: string
  emoji?: string
  /** 形如 /static/products/snacks-1.jpg，走 Vite 代理到后端静态目录 */
  image?: string
  /** 大类 code */
  category?: string
  /** 细分小类 code */
  sub?: string
  detail?: ApiDetail | null
}

export interface ApiSub {
  id: string
  name: string
  /** 该细分下的商品件数，用于「补货中」置灰 */
  count: number
}

export interface ApiCategory {
  id: string
  name: string
  hint?: string
  /** 内联 SVG 标记，后端种子里存的就是这段字符串 */
  glyph?: string
  productCount: number
  subs: ApiSub[]
  items: ApiProduct[]
  currentSub?: string | null
  /** 指定的细分没货时，后端退回整类并置 true */
  fallback?: boolean
}

export interface ApiHome {
  brand?: { name?: string; slogan?: string } | null
  categories: ApiCategory[]
  featured: ApiProduct[]
  freeShippingAt?: number
  shippingText?: string
}

export interface ApiPage<T> {
  list: T[]
  total: number
  page: number
  size: number
  pages: number
}

/** 详情接口把 ProductVO 的字段平铺 + 一栏同类推荐，不是嵌套结构 */
export interface ApiProductDetail extends ApiProduct {
  siblings?: ApiProduct[]
}

export const productApi = {
  home: () => http.get<ApiHome>('/c/home'),

  categories: () => http.get<ApiCategory[]>('/c/categories'),

  /** 不带 sub 时返回整类全部在售商品 */
  category: (code: string, sub?: string) =>
    http.get<ApiCategory>(withQuery(`/c/categories/${encodeURIComponent(code)}`, { sub })),

  search: (query: { keyword?: string; category?: string; sub?: string; sort?: string; page?: number; size?: number }) =>
    http.get<ApiPage<ApiProduct>>(withQuery('/c/products', query)),

  product: (code: string) => http.get<ApiProductDetail>(`/c/products/${encodeURIComponent(code)}`),
}
