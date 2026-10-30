import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { round2 } from '@/utils/format'
import { MAX_QTY, type CartInput } from './cart'

/**
 * 待购买清单（准备下单的那一单）
 *
 * 和「购物车」是两个完全独立的存储：
 *   owl.cart.v1     购物车 —— 长期存放，逛的时候往里扔
 *   owl.checkout.v1 待购买 —— 临时，只有你主动结算/立即购买才会进来
 *
 * 这样加入购物车不会污染当前这一单，反过来清空待购买也不会动到购物车。
 * 同一个商品可以同时存在于两边，数量互不影响。
 */

const STORAGE_KEY = 'owl.checkout.v1'

export interface CheckoutLine {
  id: string
  name: string
  spec: string
  price: number
  /** 划线价 */
  list: number
  emoji: string
  /** 商品主图 URL，没有则回落到 emoji */
  image?: string
  qty: number
}

function restore(): CheckoutLine[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed: unknown = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.flatMap((row): CheckoutLine[] => {
      if (!row || typeof row !== 'object') return []
      const r = row as Record<string, unknown>
      const price = Number(r.price)
      const qty = Number(r.qty)
      if (typeof r.id !== 'string' || !r.id || !Number.isFinite(price)) return []
      return [
        {
          id: r.id,
          name: String(r.name ?? ''),
          spec: String(r.spec ?? ''),
          price,
          list: Number.isFinite(Number(r.list)) ? Number(r.list) : price,
          emoji: String(r.emoji ?? '🛍️'),
          image: typeof r.image === 'string' ? r.image : undefined,
          qty: Number.isFinite(qty) ? Math.min(MAX_QTY, Math.max(1, Math.round(qty))) : 1,
        },
      ]
    })
  } catch {
    return []
  }
}

export const useCheckoutStore = defineStore('checkout', () => {
  const lines = ref<CheckoutLine[]>(restore())

  watch(
    lines,
    (value) => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
      } catch {
        /* 隐身模式写不进去，忽略 */
      }
    },
    { deep: true },
  )

  const count = computed(() => lines.value.reduce((sum, line) => sum + line.qty, 0))
  const amount = computed(() => round2(lines.value.reduce((s, l) => s + l.price * l.qty, 0)))
  const saved = computed(() =>
    round2(lines.value.reduce((sum, l) => sum + Math.max(0, l.list - l.price) * l.qty, 0)),
  )
  const empty = computed(() => lines.value.length === 0)

  function push(input: CartInput, qty: number) {
    const hit = lines.value.find((line) => line.id === input.id)
    if (hit) {
      hit.qty = Math.min(MAX_QTY, hit.qty + qty)
      return
    }
    lines.value.unshift({ ...input, qty: Math.min(MAX_QTY, Math.max(1, qty)) })
  }

  function add(input: CartInput, qty = 1) {
    push(input, Math.max(1, Math.round(qty)))
  }

  /** 购物车结算时整批搬过来 */
  function addMany(rows: CheckoutLine[]) {
    for (const row of rows) push(row, row.qty)
  }

  function remove(id: string) {
    lines.value = lines.value.filter((line) => line.id !== id)
  }

  function setQty(id: string, qty: number) {
    const hit = lines.value.find((line) => line.id === id)
    if (hit) hit.qty = Math.min(MAX_QTY, Math.max(1, Math.round(qty)))
  }

  function increase(id: string) {
    const hit = lines.value.find((line) => line.id === id)
    if (hit) hit.qty = Math.min(MAX_QTY, hit.qty + 1)
  }

  function decrease(id: string) {
    const hit = lines.value.find((line) => line.id === id)
    if (hit && hit.qty > 1) hit.qty -= 1
  }

  function clear() {
    lines.value = []
  }

  return {
    lines,
    count,
    amount,
    saved,
    empty,
    add,
    addMany,
    remove,
    setQty,
    increase,
    decrease,
    clear,
  }
})
