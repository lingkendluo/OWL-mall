/**
 * OWL 购物分析助手 —— 「大脑」的唯一出口。
 *
 * 现在没有后端，requestReply() 走 localReply()，用本地商品数据 + 真实购物车状态拼回复；
 * 等大模型接好，只要在 .env.local 里补一行
 *   VITE_ASSISTANT_API=https://你的服务/assistant
 * 就会自动切到远程分支，组件与 store 一行都不用动。
 *
 * ── 远程契约 ────────────────────────────────────────────────
 * POST `${VITE_ASSISTANT_API}/chat`
 * 请求体：
 *   {
 *     model:    'deepseek-chat',           // VITE_ASSISTANT_MODEL
 *     messages: [{ role: 'system'|'user'|'assistant', content: string }, ...],
 *     context:  AssistContext              // 购物车实时状态，服务端可直接拿去算凑单
 *   }
 * 响应体（任选其一，pickReply 都认）：
 *   { "reply": "..." } | { "content": "..." } | { "data": { "reply": "..." } }
 *
 * 想改成 SSE 流式：只需重写 remoteReply 里读响应那一段，把 return 改成
 * 逐块 onChunk 回调；对外签名 requestReply(turn, { signal, onChunk }) 已经是
 * 按「可能流式」留的位置，组件侧不关心。
 * ────────────────────────────────────────────────────────────
 */

import { money, round2 } from '@/utils/format'

export type ChatRole = 'user' | 'assistant'

export interface ChatMessage {
  id: string
  role: ChatRole
  text: string
  at: number
}

/** 发给模型 / 本地引擎的购物车快照，字段都是现算的，不用二次查询 */
export interface AssistContext {
  cartCount: number
  cartAmount: number
  cartCheckedAmount: number
  cartSaved: number
  toFreeShipping: number
  isFreeShipping: boolean
  freeShippingAt: number
  lines: Array<{ name: string; qty: number; price: number }>
}

export interface AssistTurn {
  question: string
  /** 本轮之前的对话，不含 question 本身 */
  history: ChatMessage[]
  context: AssistContext
}

const API_BASE = (import.meta.env.VITE_ASSISTANT_API ?? '').trim().replace(/\/+$/, '')

/** true = 已配后端，false = 本地演示模式。UI 可据此改「演示」角标 */
export const REMOTE_ENABLED = API_BASE.length > 0

export const SYSTEM_PROMPT = [
  '你是 OWL 商城的购物分析助手，风格像一位懂行的店员：简洁、有分寸，不推销。',
  '回答优先使用 context 里购物车的真实数据，给出可执行的动作：还差多少免运费、加哪件最划算、实际省了多少。',
  '不要编造价格、库存与活动；context 里没有的信息就直说不知道。',
  '回复用纯文本，可以用「·」列举，不要用 Markdown 标题或代码块。',
].join('\n')

/** 面板打开时的第一条消息；传了昵称就像在跟具体的人说话 */
export function welcomeFor(name?: string): string {
  return [
    name ? `${name}，我是 OWL 购物分析助手。` : '我是 OWL 购物分析助手。',
    '',
    '能帮你做三件事：',
    '· 算凑单 —— 离免运费还差多少，加什么最划算',
    '· 读购物车 —— 合计、已省金额、件数一目了然',
    '· 挑商品 —— 按预算从 14 个大类里筛',
    '',
    '直接说需求，或者点下面的问题试试。',
  ].join('\n')
}

export const QUICK_PROMPTS = [
  '帮我凑到免运费',
  '我的购物车能省多少',
  '这周有什么值得买',
  '配送要多久',
]

/* ============================ 对外唯一入口 ============================ */

export async function requestReply(turn: AssistTurn, signal?: AbortSignal): Promise<string> {
  if (REMOTE_ENABLED) return remoteReply(turn, signal)

  // 本地模式也留一点「思考」时间，否则打字机指示器一闪而过，反而不像在对话
  await wait(360 + Math.random() * 420, signal)
  return localReply(turn)
}

/* ============================== 远程分支 ============================== */

async function remoteReply(turn: AssistTurn, signal?: AbortSignal): Promise<string> {
  const res = await fetch(`${API_BASE}/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    signal,
    body: JSON.stringify({
      model: import.meta.env.VITE_ASSISTANT_MODEL ?? 'default',
      messages: [
        { role: 'system', content: SYSTEM_PROMPT },
        ...turn.history.slice(-10).map((m) => ({ role: m.role, content: m.text })),
        { role: 'user', content: turn.question },
      ],
      context: turn.context,
    }),
  })

  if (!res.ok) throw new Error(`接口返回 ${res.status}`)
  const payload: unknown = await res.json()
  const text = pickReply(payload)
  if (!text) throw new Error('接口返回为空')
  return text
}

/** 兼容几种常见返回结构，省得接后端时还要改这里 */
function pickReply(payload: unknown): string {
  if (typeof payload === 'string') return payload.trim()
  if (!payload || typeof payload !== 'object') return ''

  const KEYS = ['reply', 'content', 'text', 'answer'] as const
  const read = (box: Record<string, unknown>): string => {
    for (const key of KEYS) {
      const value = box[key]
      if (typeof value === 'string' && value.trim()) return value.trim()
    }
    return ''
  }

  const root = payload as Record<string, unknown>
  const direct = read(root)
  if (direct) return direct

  const { data } = root
  return data && typeof data === 'object' ? read(data as Record<string, unknown>) : ''
}

/* ============================== 本地分支 ============================== */

/**
 * 商品目录只在「推荐 / 凑单」这两条分支用得到，所以做成动态 import：
 * 否则首页整份目录会被并进主包，登录页也得跟着下载。
 */
async function loadCatalog() {
  const { FEATURED } = await import('@/views/home/catalog')
  return FEATURED
}

async function localReply({ question, context }: AssistTurn): Promise<string> {
  const q = question.trim()

  if (/^(你好|您好|hi|hello|嗨|在吗)/i.test(q)) return greet()
  if (/凑单|免运费|运费|包邮|邮费/.test(q)) return shipAdvice(context)
  if (/购物车|清单|几件|合计|总价|多少钱/.test(q)) return cartSummary(context)
  if (/省|优惠|便宜|划算|折扣|比价|值不值/.test(q)) return savingAdvice(context)
  if (/推荐|买什么|有什么|好货|挑|选什么|值得买/.test(q)) return recommend()
  if (/配送|多久|到货|送达|自提|几点|营业/.test(q)) return deliveryInfo()
  if (/退|换|售后|发票|保修/.test(q)) return afterSaleInfo()
  if (/对比|区别|哪个好/.test(q)) return compareGuide()
  return fallback()
}

function greet(): string {
  return '在。\n\n你可以直接问我购物车的情况，也可以让我按预算挑货。\n试试「帮我凑到免运费」。'
}

async function shipAdvice(ctx: AssistContext): Promise<string> {
  if (ctx.cartCount === 0) {
    return `购物车还是空的，先挑几件吧。\n\n顺便提醒：单笔满 ¥${ctx.freeShippingAt} 免 ¥5 配送费，差一点点的时候加一件小件最划算。`
  }

  if (ctx.isFreeShipping) {
    return `已经免运费了 —— 已选 ¥${money(ctx.cartCheckedAmount)}，过了 ¥${ctx.freeShippingAt} 门槛。\n\n这单符合极速达条件，最快 1 小时内送到。\n想再省的话，我可以帮你挑划线价差得最大的那几件。`
  }

  const gap = ctx.toFreeShipping
  const picks = await fillers(gap)

  if (picks.length === 0) {
    return `离免运费还差 ¥${money(gap)}（已选 ¥${money(ctx.cartCheckedAmount)} / ¥${ctx.freeShippingAt}）。\n\n随便加一件小件就够，挑你用得上的就行。`
  }

  const rows = picks.map((p) => `· ${p.name}　¥${money(p.price)}`).join('\n')
  const [first] = picks
  // 只按「最便宜那件」算，因为加一件就已经过门槛了，另一件是备选
  const after = round2(ctx.cartCheckedAmount + (first?.price ?? 0))

  return [
    `离免运费还差 ¥${money(gap)}（已选 ¥${money(ctx.cartCheckedAmount)} / ¥${ctx.freeShippingAt}）。`,
    '',
    '加一件就够，这几样最接近差额：',
    rows,
    '',
    `只加第一件的话合计 ¥${money(after)}，运费直接省掉 ¥5。`,
  ].join('\n')
}

function cartSummary(ctx: AssistContext): string {
  if (ctx.cartCount === 0) {
    return '购物车还空着。\n\n想从哪一类开始？零食、水果、日用我都能挑，说个预算或者口味就行。'
  }

  const rows = ctx.lines
    .map((line) => `· ${line.name} × ${line.qty}　¥${money(line.price * line.qty)}`)
    .join('\n')
  const ship = ctx.isFreeShipping
    ? '已免运费'
    : `再买 ¥${money(ctx.toFreeShipping)} 免运费`

  return [
    `现在车里有 ${ctx.cartCount} 件，合计 ¥${money(ctx.cartAmount)}。`,
    '',
    rows,
    '',
    `相对划线价一共省了 ¥${money(ctx.cartSaved)}，${ship}。`,
  ].join('\n')
}

function savingAdvice(ctx: AssistContext): string {
  if (ctx.cartCount === 0) {
    return '车是空的，暂时没得算。\n\n一般规律：同品类里带「会员价」标签的那几件，比划线价低 15%~30%，优先看它们。'
  }

  if (ctx.cartSaved <= 0) {
    return `这单 ¥${money(ctx.cartAmount)}，商品基本贴着划线价，暂时没什么可省的。\n\n建议先凑到 ¥${ctx.freeShippingAt} 把 ¥5 运费免掉 —— 还差 ¥${money(ctx.toFreeShipping)}。`
  }

  const raw = ctx.cartAmount / (ctx.cartAmount + ctx.cartSaved)
  const rate = Math.max(0.1, Math.round(raw * 100) / 10).toFixed(1)

  return [
    `这单比划线价省了 ¥${money(ctx.cartSaved)}，折算大约 ${rate} 折。`,
    '',
    '还有两处可以再抠：',
    `· ${ctx.isFreeShipping ? '运费已经免了，不用再凑' : `差 ¥${money(ctx.toFreeShipping)} 免运费，加一件小件就够`}`,
    '· 会员价商品优先，通常比划线价低两成',
  ].join('\n')
}

async function recommend(): Promise<string> {
  const featured = await loadCatalog()
  const rows = featured
    .slice(0, 4)
    .map((p) => `· ${p.name}　¥${money(p.price)}　${p.tag}`)
    .join('\n')

  return `这几件最近走得最好：\n\n${rows}\n\n想缩小范围的话告诉我预算或用途（比如「100 以内的早餐」），我再筛一遍。`
}

function deliveryInfo(): string {
  return [
    '三种配送方式：',
    '',
    '· 极速达　最快 1 小时，满 ¥99 免运费，不满收 ¥5',
    '· 次日达　今晚 22:00 前下单，明日 14:00 前到，大件专用',
    '· 门店自提　杭州文三路店，下单后 2 小时可取',
    '',
    '营业时间 08:00 - 22:00，现在下单今天就能送到。',
  ].join('\n')
}

function afterSaleInfo(): string {
  return [
    '· 生鲜类：签收 24 小时内可申请售后，拍照上传即可',
    '· 其余商品：7 天无理由，吊牌与包装需完好',
    '· 发票：个人中心 → 常用功能 → 发票管理，支持电子普票',
    '',
    '要走售后的话，在「个人中心 - 我的订单」里找到那笔单，点进去就能申请。',
  ].join('\n')
}

function compareGuide(): string {
  return [
    '比价我一般看三条：',
    '',
    '1. 单位价格 —— 折算到每 100g 或每件，别只看总价',
    '2. 划线价是否真实 —— 常年挂着的划线价不算优惠',
    '3. 配送成本 —— 差 ¥5 运费就够再添一件了',
    '',
    '把要对比的两件名字发我，我按这三条给你排。',
  ].join('\n')
}

function fallback(): string {
  return [
    '这个问题我还没学会回答。',
    '',
    '现在拿手的是：',
    '· 算凑单，离免运费还差多少',
    '· 读购物车，报合计和已省金额',
    '· 报配送时效、售后与发票规则',
    '· 按分类推荐商品',
    '',
    '换个问法试试？',
  ].join('\n')
}

/** 挑两件刚好补上差价的：优先「单价 ≥ 差额」里最便宜的，避免越凑越多 */
async function fillers(gap: number): Promise<Array<{ name: string; price: number }>> {
  const featured = await loadCatalog()
  const pool = [...featured].sort((a, b) => a.price - b.price)
  const enough = pool.filter((p) => p.price >= gap)
  // 有够门槛的就只推够门槛的（按价格从低到高），一件都不够才退回全场最便宜的
  const picked = (enough.length > 0 ? enough : pool).slice(0, 2)
  return picked.map((p) => ({ name: p.name, price: p.price }))
}

/* ================================ 工具 ================================ */

function wait(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException('aborted', 'AbortError'))
      return
    }
    const timer = window.setTimeout(() => {
      signal?.removeEventListener('abort', onAbort)
      resolve()
    }, ms)
    function onAbort() {
      window.clearTimeout(timer)
      reject(new DOMException('aborted', 'AbortError'))
    }
    signal?.addEventListener('abort', onAbort, { once: true })
  })
}
