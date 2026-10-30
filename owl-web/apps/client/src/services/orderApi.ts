/**
 * 订单接口，对应 owl-server 的 TradeController（/api/v1/trade/**）。
 * 下单只传 productId + qty，价格服务端查库重算，前端显示价仅供参考。
 */
import { http, withQuery } from '@shared/http'

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
  pages: number
}

export interface OrderBrief {
  orderNo: string
  /** 10 待付款 20 待发货 30 待收货 40 已完成 50 已取消 60 退款中 70 已退款 */
  status: number
  statusText: string
  payAmount: number
  totalQty: number
  firstItemName: string
  coverImage: string | null
  createdAt: number
  /** 待付款订单的支付截止时间 */
  expireAt: number | null
}

export interface OrderCounts {
  unpaid: number
  shipping: number
  review: number
  after: number
}

export interface OrderCreatePayload {
  lines: Array<{ productId: string; qty: number }>
  addressId?: string
  couponId?: string
  remark?: string
  /** 1 极速达 2 次日达 3 自提 */
  deliveryType?: number
  /** 从购物车结算传 true，详情页立即购买传 false */
  fromCart?: boolean
  /** 幂等键 */
  clientToken?: string
}

export interface OrderCreateResult {
  orderNo: string
  payNo: string
  payAmount: number
  status: number
  expireAt: number
}

export type OrderStatusFilter = 'all' | 'unpaid' | 'undelivered' | 'shipping' | 'finished' | 'cancelled'

export const orderApi = {
  create: (payload: OrderCreatePayload) => http.post<OrderCreateResult>('/trade/orders', payload),

  list: (status: OrderStatusFilter = 'all', page = 1, size = 10) =>
    http.get<PageResult<OrderBrief>>(
      withQuery('/trade/orders', { status, page, size }),
    ),

  counts: () => http.get<OrderCounts>('/trade/orders/counts'),

  cancel: (orderNo: string) =>
    http.post<OrderBrief>(`/trade/orders/${encodeURIComponent(orderNo)}/cancel`),

  confirm: (orderNo: string) =>
    http.post<OrderBrief>(`/trade/orders/${encodeURIComponent(orderNo)}/confirm`),
}
