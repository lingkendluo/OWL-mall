/**
 * 收货地址接口，对应 owl-server 的 AddressController（/api/v1/m/addresses）。
 * 后端是省/市/区三段，前端页面用「浙江省 杭州市 西湖区」一个输入框，转换在 store 里做。
 */
import { http } from '@shared/http'

export interface ApiAddress {
  id: string
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  tag: string | null
  isDefault: boolean
}

export interface AddressPayload {
  receiver: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  tag?: string
  isDefault?: boolean
}

export const addressApi = {
  list: () => http.get<ApiAddress[]>('/m/addresses'),
  create: (payload: AddressPayload) => http.post<ApiAddress>('/m/addresses', payload),
  update: (id: string, payload: AddressPayload) =>
    http.put<ApiAddress>(`/m/addresses/${encodeURIComponent(id)}`, payload),
  remove: (id: string) => http.del<void>(`/m/addresses/${encodeURIComponent(id)}`),
  setDefault: (id: string) => http.put<ApiAddress>(`/m/addresses/${encodeURIComponent(id)}/default`),
}
