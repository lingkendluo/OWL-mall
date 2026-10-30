/**
 * 会员认证接口，对应 owl-server 的 AuthController（/api/v1/auth/**）。
 *
 * UserVO 的字段名与前端 AuthUser 逐字对应，后端注释里明确写了「一个都不许改名」，
 * 所以这里不做二次映射，只在 normalizeUser 里兜一下空值。
 */
import { http } from '@shared/http'

export interface ApiUser {
  id: string
  name: string
  phone: string
  email: string
  /** black / normal / guest */
  level: string
  memberNo: string
  points: number
  coupons: number
  /** 余额，元 */
  balance: number
  /** 注册时间，毫秒戳 */
  joinedAt: number
  isGuest: boolean
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  /** access 有效期，秒 */
  expiresIn: number
  user: ApiUser
}

export interface RegisterPayload {
  name: string
  phone: string
  code: string
  password: string
  agree: boolean
}

export const authApi = {
  login: (account: string, password: string, remember: boolean) =>
    http.post<LoginResult>('/auth/login', { account, password, remember }),

  register: (payload: RegisterPayload) => http.post<LoginResult>('/auth/register', payload),

  guest: () => http.post<LoginResult>('/auth/guest'),

  /** dev 环境后端会把验证码回传，prod 只有 expireIn */
  smsCode: (phone: string) =>
    http.post<{ expireIn: number; code?: string }>('/auth/sms-code', { phone, scene: 'register' }),

  me: () => http.get<ApiUser>('/auth/me'),

  logout: () => http.post<void>('/auth/logout'),
}
