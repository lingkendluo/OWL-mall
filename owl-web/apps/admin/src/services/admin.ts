/**
 * 管理端接口清单，路径与 owl-server 的 Controller 一一对应。
 * 类型都是从后端的 VO 手抄过来的，字段名保持一致，不做二次映射。
 */
import { http, withQuery } from '@shared/http'
import type { NoticeType } from '@shared/services/notices'

export type { NoticeType }

export interface AdminProfile {
  id: string
  username: string
  nickname: string
  /** super / ops */
  role: string
}

export interface AdminLoginResult {
  accessToken: string
  /** 有效期，秒 */
  expiresIn: number
  admin: AdminProfile
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
  pages: number
}

export interface SubCategory {
  id: string
  name: string
  count: number
}

export interface Category {
  id: string
  name: string
  hint: string
  glyph: string
  productCount: number
  subs: SubCategory[]
}

/** 商品页顶部大类索引的统计项，来自 GET /admin/products/stats */
export interface CategoryStat {
  code: string
  name: string
  glyph: string
  total: number
  onSale: number
  offSale: number
}

export interface ProductDetail {
  slogan?: string
  intro?: string
  facts?: Record<string, string>
  tips?: string[]
}

export interface AdminProduct {
  /** 等于 code，如 snacks-1 */
  id: string
  name: string
  categoryCode: string
  subCode: string
  spec: string
  price: number
  list: number
  tag: string
  emoji: string
  image: string
  stock: number
  sales: number
  /** 1 上架 0 下架 */
  status: number
  isFeatured: boolean
  detail?: ProductDetail
}

/** 上架/改商品提交体，price/list 单位是元 */
export interface ProductPayload {
  name: string
  categoryCode: string
  subCode: string
  spec?: string
  price: number
  list: number
  tag?: string
  emoji?: string
  image?: string
  stock?: number
  isFeatured?: boolean
  detail?: ProductDetail
}

export interface Stats {
  gmv: number
  orderCount: number
  userCount: number
  productCount: number
  lowStockCount: number
  todayOrderCount: number
  todayGmv: number
}

/** 0 草稿 1 已发布 2 已下线 */
export type NoticeStatus = 0 | 1 | 2

export interface Notice {
  id: string
  title: string
  body: string
  type: NoticeType
  typeText: string
  pinned: boolean
  status: NoticeStatus
  statusText: string
  publishAt: number | null
  time: string
  createdAt: number | null
  updatedAt: number | null
}

export interface NoticePayload {
  title: string
  body: string
  type?: NoticeType
  pinned?: boolean
  publishNow?: boolean
}

/* ---------------- 操作日志 ---------------- */

export interface AdminLog {
  id: string
  adminName: string
  action: string
  target: string
  detail: string
  ip: string
  createdAt: number | null
  time: string
}

/* ---------------- 会员 ---------------- */

export interface AdminMember {
  id: string
  nickname: string
  phone: string
  email: string
  level: string
  levelText: string
  memberNo: string
  points: number
  balance: number
  /** 1 正常 0 禁用 */
  status: number
  guest: boolean
  createdAt: number | null
  lastLoginAt: number | null
  time: string
}

/* ---------------- 库存流水 ---------------- */

export interface StockLog {
  id: string
  changeQty: number
  /** 1 下单扣减 2 取消回补 3 采购入库 4 后台调整 */
  type: number
  typeText: string
  beforeQty: number
  afterQty: number
  refNo: string
  createdAt: number | null
  time: string
}

/* ---------------- 订单 ---------------- */

export interface AdminOrder {
  orderNo: string
  status: number
  statusText: string
  payAmount: number
  totalQty: number
  firstItemName: string
  coverImage: string
  createdAt: number | null
  expireAt: number | null
}

export interface OrderAmount {
  goodsAmount: number
  discount: number
  shipFee: number
  payable: number
  saved: number
}

export interface OrderItem {
  id: string
  name: string
  spec: string
  image: string
  price: number
  list: number
  qty: number
  total: number
}

export interface OrderAddress {
  id: string
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  tag: string
  isDefault: boolean
}

export interface OrderTimeline {
  status: number
  statusText: string
  at: number | null
  remark: string
}

export interface AdminOrderDetail {
  orderNo: string
  status: number
  statusText: string
  payStatus: number
  amounts: OrderAmount
  items: OrderItem[]
  address: OrderAddress | null
  deliveryType: number
  deliveryText: string
  remark: string
  payNo: string
  createdAt: number | null
  payTime: number | null
  deliverTime: number | null
  receiveTime: number | null
  closeTime: number | null
  cancelReason: string
  timeline: OrderTimeline[]
}

/** 后台订单筛选：字符串别名或数字状态码 */
export type OrderFilter = 'all' | 'unpaid' | 'undelivered' | 'shipping' | 'finished' | 'cancelled' | 'refund' | number

export const ORDER_STATUS: Array<{ value: number; label: string }> = [
  { value: 10, label: '待付款' },
  { value: 20, label: '待发货' },
  { value: 30, label: '待收货' },
  { value: 40, label: '已完成' },
  { value: 50, label: '已取消' },
  { value: 60, label: '退款中' },
  { value: 70, label: '已退款' },
]

export const adminApi = {
  /* ---------------- 登录 ---------------- */
  login: (username: string, password: string) =>
    http.post<AdminLoginResult>('/admin/auth/login', { username, password }),

  me: () => http.get<AdminProfile>('/admin/auth/me'),

  /* ---------------- 概览 ---------------- */
  stats: () => http.get<Stats>('/admin/stats/overview'),

  /* ---------------- 类目（上架时选细分用） ---------------- */
  categories: () => http.get<Category[]>('/admin/categories'),

  /* ---------------- 商品 ---------------- */
  products: (query: {
    keyword?: string
    status?: number
    /** 大类 code；空表示不限，对应顶部大类索引 */
    category?: string
    page?: number
    size?: number
  }) => http.get<PageResult<AdminProduct>>(withQuery('/admin/products', query)),

  /** 每个大类的商品总数与在售/下架拆分，用于索引条上的数字 */
  productStats: () => http.get<CategoryStat[]>('/admin/products/stats'),

  createProduct: (payload: ProductPayload) => http.post<AdminProduct>('/admin/products', payload),

  updateProduct: (code: string, payload: ProductPayload) =>
    http.put<AdminProduct>(`/admin/products/${encodeURIComponent(code)}`, payload),

  deleteProduct: (code: string) => http.del<void>(`/admin/products/${encodeURIComponent(code)}`),

  /** 1 上架 0 下架 */
  changeProductStatus: (code: string, status: 0 | 1) =>
    http.patch<AdminProduct>(`/admin/products/${encodeURIComponent(code)}/status`, { status }),

  adjustStock: (code: string, delta: number, reason?: string) =>
    http.patch<AdminProduct>(`/admin/products/${encodeURIComponent(code)}/stock`, { delta, reason }),

  /** 某商品的库存流水 */
  stockLogs: (code: string, query: { page?: number; size?: number } = {}) =>
    http.get<PageResult<StockLog>>(
      withQuery(`/admin/products/${encodeURIComponent(code)}/stock-logs`, query),
    ),

  /* ---------------- 订单 ---------------- */
  orders: (query: { status?: OrderFilter; keyword?: string; page?: number; size?: number } = {}) =>
    http.get<PageResult<AdminOrder>>(withQuery('/admin/orders', query)),

  orderDetail: (orderNo: string) =>
    http.get<AdminOrderDetail>(`/admin/orders/${encodeURIComponent(orderNo)}`),

  deliver: (orderNo: string, company?: string, trackingNo?: string) =>
    http.post<AdminOrderDetail>(`/admin/orders/${encodeURIComponent(orderNo)}/deliver`, {
      company,
      trackingNo,
    }),

  /* ---------------- 会员 ---------------- */
  members: (query: { keyword?: string; status?: number; page?: number; size?: number } = {}) =>
    http.get<PageResult<AdminMember>>(withQuery('/admin/members', query)),

  /** 1 正常 0 禁用 */
  changeMemberStatus: (id: string, status: 0 | 1) =>
    http.patch<AdminMember>(`/admin/members/${id}/status`, { status }),

  /* ---------------- 操作日志 ---------------- */
  logs: (query: { keyword?: string; action?: string; page?: number; size?: number } = {}) =>
    http.get<PageResult<AdminLog>>(withQuery('/admin/logs', query)),

  /* ---------------- 公告 ---------------- */
  notices: (query: { keyword?: string; status?: number } = {}) =>
    http.get<Notice[]>(withQuery('/admin/notices', query)),

  createNotice: (payload: NoticePayload) => http.post<Notice>('/admin/notices', payload),

  updateNotice: (id: string, payload: NoticePayload) =>
    http.put<Notice>(`/admin/notices/${id}`, payload),

  publishNotice: (id: string) => http.post<Notice>(`/admin/notices/${id}/publish`),

  offlineNotice: (id: string) => http.post<Notice>(`/admin/notices/${id}/offline`),

  deleteNotice: (id: string) => http.del<void>(`/admin/notices/${id}`),
}

export const NOTICE_TYPES: Array<{ value: NoticeType; label: string }> = [
  { value: 1, label: '公告' },
  { value: 2, label: '活动' },
  { value: 3, label: '维护' },
  { value: 4, label: '上新' },
]
