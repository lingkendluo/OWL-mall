import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { adminApi, type AdminProfile } from '@/services/admin'
import { ApiError, setTokenSource } from '@shared/http'

/**
 * 管理端登录态。与 C 端的 auth store 完全隔离：
 * 那边存的是「演示用会员」，这边存的是真后端签发的 admin JWT，
 * 两套 key 前缀也不一样（owl.admin.* vs owl.auth.*），互不干扰。
 */

const SESSION_KEY = 'owl.admin.session.v1'

interface Session {
  token: string
  /** 毫秒戳，过期就当没登录 */
  expiresAt: number
  admin: AdminProfile
}

function readSession(): Session | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<Session>
    if (!parsed.token || !parsed.admin || typeof parsed.expiresAt !== 'number') return null
    return parsed as Session
  } catch {
    return null
  }
}

function writeSession(session: Session | null) {
  try {
    if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    else localStorage.removeItem(SESSION_KEY)
  } catch {
    /* 隐身模式忽略 */
  }
}

export const useAdminSession = defineStore('adminSession', () => {
  const session = ref<Session | null>(readSession())

  const admin = computed(() => session.value?.admin ?? null)
  const isLoggedIn = computed(() => {
    const current = session.value
    return current !== null && current.expiresAt > Date.now()
  })
  const roleLabel = computed(() => (admin.value?.role === 'super' ? '超级管理员' : '运营'))

  /** 请求层从这里取 token，避免 services 依赖 store */
  setTokenSource(() => (isLoggedIn.value ? session.value?.token ?? null : null))

  function clear() {
    session.value = null
    writeSession(null)
  }

  async function login(username: string, password: string) {
    const result = await adminApi.login(username.trim(), password)
    const next: Session = {
      token: result.accessToken,
      // 后端给的是秒，留 30 秒余量，避免刚好卡在过期点上
      expiresAt: Date.now() + Math.max(60, result.expiresIn - 30) * 1000,
      admin: result.admin,
    }
    session.value = next
    writeSession(next)
    return result.admin
  }

  /**
   * 用已存的 token 换一次 /me：token 被服务端作废或换密钥时能立刻失效，
   * 而不是等到第一个业务请求才报 401。
   */
  async function bootstrap() {
    if (!isLoggedIn.value) {
      clear()
      return false
    }
    try {
      const profile = await adminApi.me()
      if (session.value) {
        session.value = { ...session.value, admin: profile }
        writeSession(session.value)
      }
      return true
    } catch (error) {
      if (error instanceof ApiError && error.unauthorized) clear()
      return false
    }
  }

  return { session, admin, isLoggedIn, roleLabel, login, logout: clear, bootstrap, clear }
})
