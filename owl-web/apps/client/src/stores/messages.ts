import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export type MessageKind = 'order' | 'promo' | 'system'

export interface Message {
  id: string
  kind: MessageKind
  title: string
  body: string
  /** 展示用时间文案 */
  time: string
  read: boolean
}

/** 演示数据：接后端后由 /api/v1/m/messages 下发 */
const SEED: Message[] = [
  {
    id: 'm1',
    kind: 'order',
    title: '订单已发出',
    body: '你购买的「谷饲西冷牛排 1kg」已由杭州文三路店拣货完成，骑手正在路上，预计 1 小时内送达。',
    time: '12 分钟前',
    read: false,
  },
  {
    id: 'm2',
    kind: 'promo',
    title: '会员日 · 生鲜低至 5 折',
    body: '本周三会员日，肉类、蔬果、水产全场低至 5 折，会员额外再减 20 元，仅限当日。',
    time: '1 小时前',
    read: false,
  },
  {
    id: 'm3',
    kind: 'order',
    title: '订单已完成',
    body: '订单 OWL20261024001 已确认收货，感谢你的支持。评价可获 50 积分。',
    time: '昨天 18:24',
    read: false,
  },
  {
    id: 'm4',
    kind: 'system',
    title: '配送范围更新',
    body: '你的收货地址所在片区已开通「极速达」，最快 1 小时送达，满 ¥99 免运费。',
    time: '昨天 09:10',
    read: true,
  },
  {
    id: 'm5',
    kind: 'promo',
    title: '你有一张 ¥30 券待使用',
    body: '面额 ¥30，满 ¥199 可用，有效期至 11 月 10 日，结算时自动匹配最优惠的一张。',
    time: '10 月 24 日',
    read: true,
  },
  {
    id: 'm6',
    kind: 'system',
    title: '账号安全提醒',
    body: '你的账号于 10 月 24 日在新设备登录，如非本人操作请及时修改密码。',
    time: '10 月 24 日',
    read: true,
  },
  {
    id: 'm7',
    kind: 'order',
    title: '退款已到账',
    body: '订单 OWL20261021007 的退款 ¥59.90 已退回原支付账户，预计 1-3 个工作日到账。',
    time: '10 月 22 日',
    read: true,
  },
]

export const useMessageStore = defineStore('messages', () => {
  const list = ref<Message[]>(SEED.map((item) => ({ ...item })))

  const unread = computed(() => list.value.filter((item) => !item.read).length)

  function unreadOf(kind: MessageKind): number {
    return list.value.filter((item) => item.kind === kind && !item.read).length
  }

  function markRead(id: string) {
    const hit = list.value.find((item) => item.id === id)
    if (hit) hit.read = true
  }

  function markAllRead() {
    for (const item of list.value) item.read = true
  }

  function remove(id: string) {
    list.value = list.value.filter((item) => item.id !== id)
  }

  return { list, unread, unreadOf, markRead, markAllRead, remove }
})
