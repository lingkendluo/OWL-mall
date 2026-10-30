import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'
import { useCartStore, FREE_SHIPPING } from './cart'
import {
  welcomeFor,
  requestReply,
  type AssistContext,
  type ChatMessage,
  type ChatRole,
} from '@/services/assistant'

/**
 * 助手面板的全局状态。
 * 放在 store 而不是组件里，是为了「翻页不断聊」：首页问一半跳去购物车，回来还在。
 * 刻意不做 localStorage 持久化 —— 现在回复来自本地演示数据，落盘反而会在接真模型后污染上下文。
 */

let seq = 0

function makeMessage(role: ChatRole, text: string): ChatMessage {
  seq += 1
  return { id: `ai-${seq}`, role, text, at: Date.now() }
}

function freshWelcome(): ChatMessage[] {
  // 联调：登录成谁就称呼谁；访客不点名，省得像在套近乎
  const auth = useAuthStore()
  const name = auth.user && !auth.user.isGuest ? auth.user.name : undefined
  return [makeMessage('assistant', welcomeFor(name))]
}

export const useAssistantStore = defineStore('assistant', () => {
  const open = ref(false)
  const sending = ref(false)
  const messages = ref<ChatMessage[]>(freshWelcome())

  /** 当前这一轮的请求控制器；clear / 连发时用它掐掉上一条，避免回复乱序插入 */
  let session: AbortController | null = null

  /** 用户已经开口了吗（决定欢迎语下方的快捷问题是否还显示） */
  const ready = computed(() => messages.value.some((m) => m.role === 'user'))

  function openPanel() {
    open.value = true
  }

  function closePanel() {
    open.value = false
  }

  function toggle() {
    open.value = !open.value
  }

  function buildContext(): AssistContext {
    const cart = useCartStore()
    return {
      cartCount: cart.count,
      cartAmount: cart.amount,
      cartCheckedAmount: cart.checkedAmount,
      cartSaved: cart.saved,
      toFreeShipping: cart.toFreeShipping,
      isFreeShipping: cart.isFreeShipping,
      freeShippingAt: FREE_SHIPPING,
      lines: cart.lines.map((line) => ({ name: line.name, qty: line.qty, price: line.price })),
    }
  }

  async function send(text: string) {
    const question = text.trim()
    if (!question || sending.value) return

    // 先留历史快照，再把我这句推进去，避免远程分支把 question 重复发两遍
    const history = messages.value.slice(-10)
    messages.value.push(makeMessage('user', question))

    session?.abort()
    session = new AbortController()
    const current = session
    sending.value = true

    try {
      const reply = await requestReply(
        { question, history, context: buildContext() },
        current.signal,
      )
      if (current !== session) return
      messages.value.push(makeMessage('assistant', reply))
    } catch (error) {
      if (current !== session) return
      const detail = error instanceof Error ? error.message : '未知错误'
      messages.value.push(
        makeMessage(
          'assistant',
          `连接助手时出了点问题：${detail}\n\n可以再问一次，或者先看看购物车。`,
        ),
      )
    } finally {
      if (current === session) sending.value = false
    }
  }

  function clear() {
    session?.abort()
    session = null
    sending.value = false
    messages.value = freshWelcome()
  }

  return { open, sending, messages, ready, openPanel, closePanel, toggle, send, clear }
})
