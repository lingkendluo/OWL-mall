import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { setTokenSource } from '@shared/http'
import { authApi, type ApiUser, type LoginResult, type RegisterPayload } from '@/services/authApi'

/**
 * 登录态（真后端版）。
 *
 * 和之前的本地演示版相比：
 *   1. 账号密码交给 POST /auth/login 校验，昵称/等级/积分/余额全部以服务端 UserVO 为准；
 *   2. 多存一份 accessToken，所有请求由 shared/http 自动带 Authorization；
 *   3. 退出调 /auth/logout 让服务端作废 refreshToken，再清本地。
 *
 * 存储结构：{ user, token, refreshToken, expiresAt, remember }，
 * 勾了「保持登录」进 localStorage，否则只进 sessionStorage。
 */

export type MemberLevel = 'black' | 'normal' | 'guest'

export interface AuthUser {
  id: string
  name: string
  phone: string
  email: string
  level: MemberLevel
  memberNo: string
  points: number
  coupons: number
  balance: number
  joinedAt: number
  isGuest: boolean
}

export const LEVEL_LABEL: Record<MemberLevel, string> = {
  black: 'OWL 黑卡会员',
  normal: 'OWL 普通会员',
  guest: '访客模式',
}

const PHONE_RE = /^1[3-9]\d{9}$/
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** 138****6688 */
export function maskPhone(phone: string): string {
  return PHONE_RE.test(phone) ? phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : ''
}

export function looksLikeAccount(value: string): boolean {
  return PHONE_RE.test(value) || EMAIL_RE.test(value)
}

const SESSION_KEY = 'owl.auth.session.v2'
const LAST_KEY = 'owl.auth.last.v1'

interface Session {
  user: AuthUser
  token: string
  refreshToken: string
  expiresAt: number
  remember: boolean
}

function safe<T>(run: () => T, fallback: T): T {
  try {
    return run()
  } catch {
    /* 隐身模式禁用存储时按空处理 */
    return fallback
  }
}

function normalizeUser(raw: ApiUser): AuthUser {
  const level: MemberLevel = raw.level === 'black' || raw.level === 'guest' ? raw.level : 'normal'
  return {
    id: String(raw.id ?? ''),
    name: (raw.name ?? '').trim() || 'OWL 用户',
    phone: raw.phone ?? '',
    email: raw.email ?? '',
    level,
    memberNo: raw.memberNo ?? '',
    points: Math.max(0, Number(raw.points ?? 0)),
    coupons: Math.max(0, Number(raw.coupons ?? 0)),
    balance: Number.isFinite(Number(raw.balance)) ? Number(raw.balance) : 0,
    joinedAt: Number(raw.joinedAt ?? Date.now()),
    isGuest: raw.isGuest === true,
  }
}

function clearStored() {
  safe(() => {
    // 只清会话，**保留 LAST_KEY** —— 退出后登录页要能把账号回填出来
    localStorage.removeItem(SESSION_KEY)
    sessionStorage.removeItem(SESSION_KEY)
  }, undefined)
}

function readSession(): Session | null {
  const raw = safe(
    () => localStorage.getItem(SESSION_KEY) ?? sessionStorage.getItem(SESSION_KEY),
    null,
  )
  if (!raw) return null
  const parsed = safe(() => JSON.parse(raw) as Partial<Session>, null)
  if (!parsed?.token || !parsed.user) return null
  // access token 过期就当没登录，免得每个请求都吃 401
  if (typeof parsed.expiresAt === 'number' && parsed.expiresAt <= Date.now()) {
    clearStored()
    return null
  }
  return {
    user: normalizeUser(parsed.user as ApiUser),
    token: parsed.token,
    refreshToken: parsed.refreshToken ?? '',
    expiresAt: parsed.expiresAt ?? 0,
    remember: parsed.remember !== false,
  }
}

function writeSession(next: Session) {
  const text = JSON.stringify(next)
  safe(() => {
    if (next.remember) {
      localStorage.setItem(SESSION_KEY, text)
      sessionStorage.removeItem(SESSION_KEY)
    } else {
      sessionStorage.setItem(SESSION_KEY, text)
      localStorage.removeItem(SESSION_KEY)
    }
  }, undefined)
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<Session | null>(readSession())
  const lastAccount = ref<string>(safe(() => localStorage.getItem(LAST_KEY) ?? '', ''))

  const user = computed(() => session.value?.user ?? null)
  const isLoggedIn = computed(() => session.value !== null)
  const isGuest = computed(() => session.value?.user.isGuest === true)
  const displayName = computed(() => session.value?.user.name ?? '访客')
  const levelLabel = computed(() =>
    session.value ? LEVEL_LABEL[session.value.user.level] : '未登录',
  )
  /** 优先显示打码手机号，没手机号就显示邮箱 */
  const contact = computed(() => {
    const current = session.value?.user
    if (!current) return ''
    return maskPhone(current.phone) || current.email || '未绑定手机号'
  })

  /** 请求层（shared/http）从这里取 token，services 不反向依赖 store */
  setTokenSource(() => session.value?.token ?? null)

  function rememberAccount(account: string) {
    lastAccount.value = account
    safe(() => localStorage.setItem(LAST_KEY, account), undefined)
  }

  function apply(result: LoginResult, remember: boolean): AuthUser {
    const nextUser = normalizeUser(result.user)
    const next: Session = {
      user: nextUser,
      token: result.accessToken,
      refreshToken: result.refreshToken,
      // 后端给秒数，留 30 秒余量，别卡在过期点上
      expiresAt: Date.now() + Math.max(60, (result.expiresIn ?? 7200) - 30) * 1000,
      remember,
    }
    session.value = next
    writeSession(next)
    return nextUser
  }

  async function signIn(account: string, password: string, remember = true): Promise<AuthUser> {
    const key = account.trim()
    if (!key) throw new Error('请输入邮箱地址或手机号')
    if (!password) throw new Error('请输入密码')
    const result = await authApi.login(key, password, remember)
    rememberAccount(key)
    return apply(result, remember)
  }

  /** 注册即登录。密码规则与后端一致：8-20 位，必须同时含字母和数字 */
  async function signUp(input: RegisterPayload, remember = true): Promise<AuthUser> {
    const name = input.name.trim()
    const phone = input.phone.trim()
    const code = input.code.trim()

    if (!name) throw new Error('请填写昵称')
    if (!PHONE_RE.test(phone)) throw new Error('请填写正确的手机号')
    if (!/^\d{6}$/.test(code)) throw new Error('请输入 6 位验证码')
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,20}$/.test(input.password)) {
      throw new Error('密码 8-20 位，必须同时包含字母和数字')
    }
    if (!input.agree) throw new Error('请先同意用户协议')

    const result = await authApi.register({ name, phone, code, password: input.password, agree: true })
    rememberAccount(phone)
    return apply(result, remember)
  }

  /** 体验模式：后端建游客账号，能逛能加购，资产清零 */
  async function signInAsGuest(): Promise<AuthUser> {
    const result = await authApi.guest()
    return apply(result, false)
  }

  async function signOut(): Promise<void> {
    try {
      if (session.value) await authApi.logout()
    } catch {
      /* 后端没起也要能退，本地清干净就行 */
    }
    session.value = null
    clearStored()
  }

  /** 刷新页面后拉一次最新资料；token 失效时由 401 兜底 */
  async function refreshMe(): Promise<void> {
    if (!session.value) return
    try {
      const me = await authApi.me()
      session.value = { ...session.value, user: normalizeUser(me) }
      writeSession(session.value)
    } catch {
      /* 静默失败，页面继续用缓存里的资料 */
    }
  }

  return {
    session,
    user,
    lastAccount,
    isLoggedIn,
    isGuest,
    displayName,
    levelLabel,
    contact,
    signIn,
    signUp,
    signInAsGuest,
    signOut,
    refreshMe,
  }
})
