/**
 * 后端请求的唯一出口。
 *
 * 全站只有管理端走网络（C 端目前仍是本地演示数据），所以这里刻意保持极简：
 * 一个 fetch 封装 + 一个错误类型，不引 axios。
 *
 * 后端约定（见 owl-server 的 Result）：
 *   业务失败也返回 HTTP 200，靠 code 区分，code === 0 才是成功。
 *   所以这里先看 HTTP 状态，再拆信封，任一环节不对都抛 ApiError。
 */

/** 开发环境走 vite 代理（/api/v1 → http://localhost:8080），生产可用 VITE_API_BASE 覆盖 */
const BASE = (import.meta.env.VITE_API_BASE ?? '/api/v1').replace(/\/+$/, '')

export class ApiError extends Error {
  constructor(
    readonly code: number,
    message: string,
    readonly status = 200,
  ) {
    super(message)
    this.name = 'ApiError'
  }

  /** token 过期/未登录，调用方据此踢回登录页 */
  get unauthorized(): boolean {
    return this.code === 10002 || this.status === 401
  }

  /** 压根没连上后端，和「后端返回了错误」要分开提示 */
  get offline(): boolean {
    return this.code === -1
  }
}

interface Envelope<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

type TokenSource = () => string | null

let readToken: TokenSource = () => null

/** 由 adminSession store 注入，避免 services 反向依赖 store */
export function setTokenSource(source: TokenSource) {
  readToken = source
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = readToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    if (body !== undefined) headers['Content-Type'] = 'application/json'
    response = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(-1, '连不上后端：请确认 owl-server 已启动在 8080 端口')
  }

  if (response.status === 401) {
    // 401 的响应体可能为空，统一给一句能看懂的话
    throw new ApiError(10002, '登录已过期，请重新登录', 401)
  }

  let payload: Envelope<T> | null = null
  try {
    payload = (await response.json()) as Envelope<T>
  } catch {
    payload = null
  }

  if (!response.ok) {
    throw new ApiError(payload?.code ?? response.status, payload?.message ?? `请求失败（${response.status}）`, response.status)
  }
  if (!payload) {
    throw new ApiError(-1, '后端返回的不是 JSON，确认代理地址是否正确')
  }
  if (payload.code !== 0) {
    throw new ApiError(payload.code, payload.message || '请求失败', response.status)
  }
  return payload.data
}

export const http = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body ?? {}),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body ?? {}),
  del: <T>(path: string) => request<T>('DELETE', path),
}

/** 拼 query，空值自动丢掉，省得每个调用点都写一遍 */
export function withQuery(path: string, query: Record<string, string | number | undefined | null>): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue
    params.set(key, String(value))
  }
  const tail = params.toString()
  return tail ? `${path}?${tail}` : path
}
