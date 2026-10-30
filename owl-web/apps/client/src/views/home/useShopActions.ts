import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'
import { useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import type { Product } from './catalog'

/**
 * 商城页面的四个动作，分类页 / 详情页共用一套语义：
 *   「+ −」→ 只改「这一单」的待购买清单（底栏合计）
 *   「加入购物车」→ 只改购物车，且是个开关
 * 首页有自己的内联 toast，所以没有复用这一段。
 */
export function useShopActions() {
  const router = useRouter()
  const cart = useCartStore()
  const pending = useCheckoutStore()
  const { show } = useToast()

  function snapshot(product: Product) {
    return {
      id: product.id,
      name: product.name,
      spec: product.spec,
      price: product.price,
      list: product.list,
      emoji: product.emoji,
      image: product.image,
    }
  }

  function pickMore(product: Product, qty: number) {
    const before = pending.lines.find((line) => line.id === product.id)?.qty ?? 0
    pending.add(snapshot(product), qty)
    if (before === 0) show(`已选中 · ${product.name}`)
  }

  function pickLess(product: Product) {
    const line = pending.lines.find((l) => l.id === product.id)
    if (!line) return
    if (line.qty > 1) pending.decrease(product.id)
    else pending.remove(product.id)
  }

  function toggleCart(product: Product) {
    const hit = cart.lines.find((line) => line.id === product.id)
    if (hit) {
      cart.remove(product.id)
      show(`已从购物车移出 · ${product.name}`)
      return
    }
    cart.add(snapshot(product), 1)
    show(`已加入购物车 · ${product.name}`)
  }

  function buyNow(product: Product) {
    if (!pending.lines.some((line) => line.id === product.id)) pending.add(snapshot(product), 1)
    void router.push({ name: 'checkout' })
  }

  function goCheckout() {
    void router.push({ name: 'checkout' })
  }

  return { pickMore, pickLess, toggleCart, buyNow, goCheckout, snapshot }
}
