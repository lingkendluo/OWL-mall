<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError } from '@shared/http'
import SubPage from '@/components/SubPage.vue'
import AppToast from '@/components/AppToast.vue'
import { useToast } from '@/composables/useToast'
import { maskPhone } from '@/stores/auth'
import { useAddressStore, type Address } from '@/stores/address'
import { FREE_SHIPPING, useCartStore } from '@/stores/cart'
import { useCheckoutStore } from '@/stores/checkout'
import { orderApi } from '@/services/orderApi'
import { money, round2 } from '@/utils/format'
import './checkout.css'

const pending = useCheckoutStore()
const cart = useCartStore()
const address = useAddressStore()
const router = useRouter()
const { show } = useToast()

/** 未满门槛收 5 元运费，与购物车保持一致 */
const SHIP_FEE = 5

const shipFee = computed(() =>
  pending.empty || pending.amount >= FREE_SHIPPING ? 0 : SHIP_FEE,
)
const payable = computed(() => round2(pending.amount + shipFee.value))

const subtitle = computed(() => {
  if (pending.empty) return '还没有待付款商品'
  // 顶栏空间窄，金额明细底部还有一条，这里只用最短的写法
  return `共 ${pending.count} 件 · ¥${money(payable.value)}`
})

/* ------------------------------ 收货地址 ------------------------------ */

/**
 * 默认用地址簿里带「默认」标记的那条；
 * 本页的「切换地址」只改这一次结算，不写回默认，免得下过一单就把偏好改了。
 */
const pickedId = ref('')
const pickerOpen = ref(false)

const chosen = computed(() => address.find(pickedId.value) ?? address.defaultAddress)

function pick(row: Address) {
  pickedId.value = row.id
  pickerOpen.value = false
  show(`本单送到「${row.tag}」`)
}

function openAddresses() {
  void router.push({ name: 'addresses' })
}

function goShop() {
  void router.push({ name: 'home' })
}

/** 走错了整单退回购物车，不丢东西 */
function backToCart() {
  if (pending.empty) return
  const count = pending.count
  for (const line of pending.lines) {
    cart.add(
      {
        id: line.id,
        name: line.name,
        spec: line.spec,
        price: line.price,
        list: line.list,
        emoji: line.emoji,
        image: line.image,
      },
      line.qty,
    )
  }
  pending.clear()
  show(`已退回 ${count} 件到购物车`)
  void router.push({ name: 'cart' })
}

const submitting = ref(false)

/** 幂等键：网络抖动重试时后端会返回同一张订单，不会重复下单 */
function newClientToken(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}

async function submit() {
  if (pending.empty || submitting.value) return
  // 没有收货地址这一单没法发，先把人送过去加一个
  if (!chosen.value) {
    show('先加一个收货地址')
    openAddresses()
    return
  }

  submitting.value = true
  try {
    const result = await orderApi.create({
      lines: pending.lines.map((line) => ({ productId: line.id, qty: line.qty })),
      addressId: chosen.value.id,
      deliveryType: 1,
      // 待付款清单不是购物车，清空清单不会影响购物车里的其他商品
      fromCart: false,
      clientToken: newClientToken(),
    })
    pending.clear()
    show(`下单成功 · 应付 ¥${money(result.payAmount)}`)
    void router.push({ name: 'orders', query: { status: 'unpaid' } })
  } catch (e) {
    show(e instanceof ApiError ? e.message : '下单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 地址簿是异步拉的，进页面时主动要一次，避免「明明有地址却提示去添加」
  void address.load()
})
</script>

<template>
  <SubPage title="待付款" :subtitle="subtitle" :show-footer="!pending.empty">
    <template v-if="!pending.empty">
      <!-- 两个清单的关系说明，别让人以为购物车被清空了 -->
      <section class="chk-note">
        <span class="chk-note-ico">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M5.6 3.6h12.8v16.8l-2.2-1.5-2.2 1.5-2.1-1.5-2.2 1.5-2.1-1.5z" />
            <path d="M9 8.6h6M9 12.2h4" />
          </svg>
        </span>
        <p>
          <b>这里是单独的一份待付款清单</b>
          <span>
            它和购物车分开存放。提交后本清单清空，购物车里的其他商品不受影响。
          </span>
        </p>
        <button class="chk-note-btn" type="button" @click="backToCart">全部退回购物车</button>
      </section>

      <!-- 收货地址：默认取地址簿里的默认地址，可在本页临时切换 -->
      <section class="chk-card chk-addr">
        <span class="chk-addr-ico">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M12 21c4.2-4 6.4-7 6.4-9.8A6.4 6.4 0 0 0 5.6 11.2C5.6 14 7.8 17 12 21Z" />
            <path d="M12 13.4a2.2 2.2 0 1 0 0-4.4 2.2 2.2 0 0 0 0 4.4Z" />
          </svg>
        </span>

        <div v-if="chosen" class="chk-addr-main">
          <p class="chk-addr-line">
            <b>{{ chosen.name }}</b>
            <span>{{ maskPhone(chosen.phone) || chosen.phone }}</span>
            <i :class="{ 'is-plain': !chosen.isDefault }">{{ chosen.isDefault ? '默认' : chosen.tag }}</i>
          </p>
          <p class="chk-addr-text">{{ chosen.region }} {{ chosen.detail }}</p>
        </div>
        <div v-else class="chk-addr-main">
          <p class="chk-addr-line"><b>还没有收货地址</b></p>
          <p class="chk-addr-text">加一个地址，才知道这单往哪儿送</p>
        </div>

        <div class="chk-addr-side">
          <em class="chk-addr-tip">最快 1 小时送达</em>
          <div class="chk-addr-acts">
            <button v-if="address.count > 1" class="chk-addr-btn" type="button" @click="pickerOpen = true">
              切换地址
            </button>
            <button class="chk-addr-btn is-ghost" type="button" @click="openAddresses">
              {{ chosen ? '管理' : '去添加' }}
            </button>
          </div>
        </div>
      </section>

      <!-- 商品 + 金额：桌面端并排，清单在左，小票贴右上跟手 -->
      <div class="chk-body">
        <div class="chk-left">
          <section class="chk-card">
            <header class="chk-head">
              <h2>待付款商品</h2>
              <span>{{ pending.count }} 件</span>
            </header>

            <ul class="chk-list">
              <li v-for="line in pending.lines" :key="line.id" class="chk-row">
                <div class="chk-thumb">
                  <img v-if="line.image" :src="line.image" :alt="line.name" loading="lazy" decoding="async" />
                  <span v-else>{{ line.emoji }}</span>
                </div>

                <div class="chk-main">
                  <p class="chk-name">{{ line.name }}</p>
                  <p class="chk-spec">{{ line.spec }}</p>
                  <p class="chk-price">
                    <small>¥</small>{{ money(line.price) }}<del>¥{{ money(line.list) }}</del>
                  </p>
                </div>

                <div class="chk-qty">
                  <button
                    type="button"
                    aria-label="减少数量"
                    :disabled="line.qty <= 1"
                    @click="pending.decrease(line.id)"
                  >
                    −
                  </button>
                  <span>{{ line.qty }}</span>
                  <button type="button" aria-label="增加数量" @click="pending.increase(line.id)">
                    ＋
                  </button>
                </div>

                <p class="chk-line-total"><small>¥</small>{{ money(line.price * line.qty) }}</p>

                <button
                  class="chk-del"
                  type="button"
                  :aria-label="`移除 ${line.name}`"
                  @click="pending.remove(line.id)"
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

        <aside class="chk-side">
          <section class="chk-card chk-summary">
            <h2>金额明细</h2>
            <dl>
              <div>
                <dt>商品合计（{{ pending.count }} 件）</dt>
                <dd>¥{{ money(pending.amount) }}</dd>
              </div>
              <div>
                <dt>已比划线价省</dt>
                <dd class="is-save">¥{{ money(pending.saved) }}</dd>
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

    <!-- 空态 -->
    <section v-else class="chk-empty">
      <span class="chk-empty-ico">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M5.6 3.6h12.8v16.8l-2.2-1.5-2.2 1.5-2.1-1.5-2.2 1.5-2.1-1.5z" />
          <path d="M9 8.6h6M9 12.2h4" />
        </svg>
      </span>
      <h2>待付款清单是空的</h2>
      <p>
        在商品卡上点「立即购买」，或者在购物车里勾选后「去结算」，商品会进到这里
      </p>
      <button class="chk-empty-btn" type="button" @click="goShop">去逛逛</button>
    </section>

    <template #footer>
      <div class="chk-foot">
        <div class="chk-foot-sum">
          <span>应付</span>
          <b><small>¥</small>{{ money(payable) }}</b>
          <em>{{ pending.count }} 件</em>
        </div>
        <button class="chk-submit" type="button" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : '提交订单' }}
        </button>
      </div>
    </template>
  </SubPage>

  <!-- 本单临时切换收货地址：选完立刻回到这单，不动地址簿里的默认 -->
  <template v-if="pickerOpen">
    <div class="chk-picker-mask" @click="pickerOpen = false" />
    <div class="chk-picker" role="dialog" aria-label="选择收货地址">
      <header class="chk-picker-head">
        <h2>选择收货地址</h2>
        <span>共 {{ address.count }} 个</span>
      </header>

      <ul class="chk-picker-list">
        <li v-for="row in address.list" :key="row.id">
          <button
            class="chk-picker-row"
            type="button"
            :class="{ 'is-on': row.id === chosen?.id }"
            @click="pick(row)"
          >
            <span class="chk-picker-dot" aria-hidden="true" />
            <span class="chk-picker-main">
              <span class="chk-picker-line">
                <b>{{ row.name }}</b>
                <em>{{ maskPhone(row.phone) || row.phone }}</em>
                <i :class="{ 'is-plain': !row.isDefault }">{{ row.isDefault ? '默认' : row.tag }}</i>
              </span>
              <span class="chk-picker-text">{{ row.region }} {{ row.detail }}</span>
            </span>
          </button>
        </li>
      </ul>

      <footer class="chk-picker-foot">
        <button class="chk-addr-btn is-ghost" type="button" @click="openAddresses">
          管理收货地址
        </button>
        <button class="chk-addr-btn" type="button" @click="pickerOpen = false">取消</button>
      </footer>
    </div>
  </template>

  <AppToast />
</template>
