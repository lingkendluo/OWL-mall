<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import SubPage from '@/components/SubPage.vue'
import AppToast from '@/components/AppToast.vue'
import { useToast } from '@/composables/useToast'
import { FREE_SHIPPING, useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { money, round2 } from '@/utils/format'
import './cart.css'

const cart = useCartStore()
const pending = useCheckoutStore()
const router = useRouter()
const { show } = useToast()

/** 未满门槛收 5 元运费 */
const SHIP_FEE = 5

const subtitle = computed(() => {
  if (cart.empty) return '还没有商品'
  return `共 ${cart.count} 件 · 已选 ${cart.checkedCount} 件`
})

const shipFee = computed(() => (cart.checkedCount > 0 && !cart.isFreeShipping ? SHIP_FEE : 0))
const payable = computed(() => round2(cart.checkedAmount + shipFee.value))
/** 只算已勾选的行，避免取消勾选后优惠还挂着 */
const checkedSaved = computed(() =>
  round2(
    cart.checkedLines.reduce((sum, line) => sum + Math.max(0, line.list - line.price) * line.qty, 0),
  ),
)

function goShop() {
  router.push({ name: 'home' })
}

function removeChecked() {
  const n = cart.checkedCount
  if (!n) {
    show('先勾选要删除的商品')
    return
  }
  cart.removeChecked()
  show(`已移除 ${n} 件商品`)
}

/**
 * 去结算 = 把勾选的商品从购物车「搬」到独立的待购买清单。
 * 搬过去之后购物车就不再持有它们，两个清单始终互不重叠；
 * 结算页给了「全部退回购物车」，走错了能原样退回。
 */
function checkout() {
  if (!cart.checkedCount) {
    show('先勾选要结算的商品')
    return
  }

  const picked = cart.checkedLines.map((line) => ({ ...line }))
  pending.addMany(picked)
  cart.removeMany(picked.map((line) => line.id))
  void router.push({ name: 'checkout' })
}
</script>

<template>
  <SubPage title="购物车" :subtitle="subtitle" :show-footer="!cart.empty">
    <!-- ========== 有货 ========== -->
    <template v-if="!cart.empty">
      <!-- 免运费进度 -->
      <section class="cart-ship">
        <div class="cart-ship-row">
          <p>
            <template v-if="cart.isFreeShipping">
              <b>已免运费</b>
              <span>本单免配送费，最快 1 小时送达</span>
            </template>
            <template v-else>
              <b>再买 ¥{{ money(cart.toFreeShipping) }} 免运费</b>
              <span>满 ¥{{ FREE_SHIPPING }} 免配送费，否则收 ¥{{ SHIP_FEE }}</span>
            </template>
          </p>
          <em>{{ cart.shippingRate }}%</em>
        </div>
        <div class="cart-ship-track">
          <i :style="{ width: `${cart.shippingRate}%` }" />
        </div>
      </section>

      <!-- 商品列表 + 明细：桌面端并排，清单在左、小票在右 -->
      <div class="cart-body">
        <div class="cart-left">
          <section class="cart-card">
            <header class="cart-toolbar">
              <label class="cart-checkbox">
                <input type="checkbox" :checked="cart.allChecked" @change="cart.toggleAll()" />
                <span>全选</span>
              </label>
              <span class="cart-toolbar-info">共 {{ cart.count }} 件，已选 {{ cart.checkedCount }} 件</span>
              <button class="cart-clear" type="button" @click="removeChecked">删除选中</button>
            </header>

            <ul class="cart-list">
              <li
                v-for="line in cart.lines"
                :key="line.id"
                class="cart-row"
                :class="{ 'is-off': !line.checked }"
              >
                <input
                  class="cart-pick"
                  type="checkbox"
                  :checked="line.checked"
                  :aria-label="`选择 ${line.name}`"
                  @change="cart.toggle(line.id)"
                />

                <div class="cart-thumb">
                  <img v-if="line.image" :src="line.image" :alt="line.name" loading="lazy" decoding="async" />
                  <span v-else>{{ line.emoji }}</span>
                </div>

                <div class="cart-main">
                  <p class="cart-name">{{ line.name }}</p>
                  <p class="cart-spec">{{ line.spec }}</p>
                  <p class="cart-price">
                    <small>¥</small>{{ money(line.price) }}<del>¥{{ money(line.list) }}</del>
                  </p>
                </div>

                <div class="cart-qty">
                  <button
                    type="button"
                    aria-label="减少数量"
                    :disabled="line.qty <= 1"
                    @click="cart.decrease(line.id)"
                  >
                    −
                  </button>
                  <span>{{ line.qty }}</span>
                  <button type="button" aria-label="增加数量" @click="cart.increase(line.id)">
                    ＋
                  </button>
                </div>

                <p class="cart-line-total"><small>¥</small>{{ money(line.price * line.qty) }}</p>

                <button
                  class="cart-del"
                  type="button"
                  :aria-label="`删除 ${line.name}`"
                  @click="cart.remove(line.id)"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.7"
                    stroke-linecap="round"
                  >
                    <path d="M5.5 7.5h13M9.5 7.5V5.2h5v2.3M7 7.5l.9 11.3h8.2L17 7.5" />
                  </svg>
                </button>
              </li>
            </ul>
          </section>
        </div>

        <aside class="cart-side">
          <section class="cart-card cart-summary">
            <h2>结算明细</h2>
            <dl>
              <div>
                <dt>商品合计（{{ cart.checkedCount }} 件）</dt>
                <dd>¥{{ money(cart.checkedAmount) }}</dd>
              </div>
              <div>
                <dt>已比划线价省</dt>
                <dd class="is-save">¥{{ money(checkedSaved) }}</dd>
              </div>
              <div>
                <dt>配送费</dt>
                <dd>{{ shipFee ? `¥${money(shipFee)}` : '免运费' }}</dd>
              </div>
              <div class="is-total">
                <dt>应付金额</dt>
                <dd><small>¥</small>{{ money(payable) }}</dd>
              </div>
            </dl>
          </section>
        </aside>
      </div>
    </template>

    <!-- ========== 空态 ========== -->
    <section v-else class="cart-empty">
      <span class="cart-empty-ico">🛒</span>
      <h2>购物车还空着</h2>
      <p>去挑点好货吧，满 ¥{{ FREE_SHIPPING }} 免运费，最快 1 小时送达</p>
      <button class="cart-empty-btn" type="button" @click="goShop">去逛逛</button>
    </section>

    <!-- ========== 底部结算条 ========== -->
    <template #footer>
      <div class="cart-foot">
        <label class="cart-checkbox">
          <input type="checkbox" :checked="cart.allChecked" @change="cart.toggleAll()" />
          <span>全选</span>
        </label>

        <div class="cart-foot-sum">
          <span>合计</span>
          <b><small>¥</small>{{ money(payable) }}</b>
          <em>已选 {{ cart.checkedCount }} 件</em>
        </div>

        <button
          class="cart-checkout"
          type="button"
          :disabled="!cart.checkedCount"
          @click="checkout"
        >
          去结算
        </button>
      </div>
    </template>
  </SubPage>

  <AppToast />
</template>
