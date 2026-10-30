<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  adminApi,
  ORDER_STATUS,
  type AdminOrder,
  type AdminOrderDetail,
  type OrderFilter,
} from '@/services/admin'
import { ApiError } from '@shared/http'
import { useAdminToast } from './useAdminToast'
import './admin.css'

/**
 * 订单管理。发货只能从「待发货(20)」走，后端 if 也卡了这道，
 * 这里提前把按钮藏掉，比让用户点了报错更好受。
 */
const { message, isError, show } = useAdminToast()

const TABS: Array<{ key: OrderFilter; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 10, label: '待付款' },
  { key: 20, label: '待发货' },
  { key: 30, label: '待收货' },
  { key: 40, label: '已完成' },
  { key: 50, label: '已取消' },
  { key: 'refund', label: '退款' },
]

const rows = ref<AdminOrder[]>([])
const total = ref(0)
const pages = ref(1)
const page = ref(1)
const pageSize = 12
const statusTab = ref<OrderFilter>('all')
const keyword = ref('')
const loading = ref(false)
const error = ref('')

const detail = reactive({
  open: false,
  loading: false,
  error: '',
  data: null as AdminOrderDetail | null,
})

const deliverDlg = reactive({
  open: false,
  orderNo: '',
  company: '顺丰',
  trackingNo: '',
  pending: false,
  error: '',
})

const pendingCount = computed(() => rows.value.filter((item) => item.status === 20).length)

/**
 * 待付款订单总数。列表是分页 + 带筛选的，不能从 rows 里数，
 * 单独查一次 status=10、size=1，只取 total。
 */
const unpaidTotal = ref(0)

async function loadUnpaidTotal() {
  try {
    const result = await adminApi.orders({ status: 10, page: 1, size: 1 })
    unpaidTotal.value = result.total
  } catch {
    // 统计失败不影响列表，角标不显示就行
    unpaidTotal.value = 0
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await adminApi.orders({
      status: statusTab.value,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: pageSize,
    })
    rows.value = result.list
    total.value = result.total
    pages.value = result.pages
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载订单失败'
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void load()
  void loadUnpaidTotal()
})

function pickTab(tab: OrderFilter) {
  statusTab.value = tab
  page.value = 1
  void load()
  void loadUnpaidTotal()
}

function go(next: number) {
  if (next < 1 || next > pages.value) return
  page.value = next
  void load()
}

function money(value: number | null | undefined): string {
  return `¥${(value ?? 0).toFixed(2)}`
}

function stamp(at: number | null): string {
  if (!at) return '—'
  const date = new Date(at)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function statusTag(status: number): string {
  if (status === 20) return 'warn'
  if (status === 30) return 'info'
  if (status === 40) return 'ok'
  if (status === 50 || status === 70) return 'off'
  return 'info'
}

async function openDetail(row: AdminOrder) {
  detail.data = null
  detail.error = ''
  detail.open = true
  detail.loading = true
  try {
    detail.data = await adminApi.orderDetail(row.orderNo)
  } catch (e) {
    detail.error = e instanceof ApiError ? e.message : '读取详情失败'
  } finally {
    detail.loading = false
  }
}

function openDeliver(row: AdminOrder | AdminOrderDetail) {
  deliverDlg.orderNo = row.orderNo
  deliverDlg.company = '顺丰'
  deliverDlg.trackingNo = ''
  deliverDlg.error = ''
  deliverDlg.open = true
}

async function submitDeliver() {
  if (deliverDlg.pending) return
  deliverDlg.error = ''
  if (!deliverDlg.trackingNo.trim()) {
    deliverDlg.error = '运单号不能为空'
    return
  }
  deliverDlg.pending = true
  try {
    await adminApi.deliver(
      deliverDlg.orderNo,
      deliverDlg.company.trim() || undefined,
      deliverDlg.trackingNo.trim(),
    )
    show('已发货，用户会收到消息通知')
    deliverDlg.open = false
    detail.open = false
    await load()
  } catch (e) {
    deliverDlg.error = e instanceof ApiError ? e.message : '发货失败'
  } finally {
    deliverDlg.pending = false
  }
}
</script>

<template>
  <section class="adm-card">
    <div class="adm-toolbar">
      <div class="adm-tabs">
        <button
          v-for="tab in TABS"
          :key="String(tab.key)"
          class="adm-tab"
          :class="{ 'is-active': statusTab === tab.key }"
          type="button"
          @click="pickTab(tab.key)"
        >
          {{ tab.label }}
          <em v-if="tab.key === 10 && unpaidTotal > 0" class="adm-tab-count">{{ unpaidTotal }}</em>
        </button>
      </div>

      <input
        v-model="keyword"
        class="adm-input is-search"
        placeholder="搜订单号 / 收货人，回车查询"
        @keyup.enter="page = 1; load()"
      />
      <button class="adm-btn sm" type="button" @click="page = 1; load()">查询</button>
    </div>

    <div v-if="error" class="adm-alert" style="margin: 12px 16px">
      {{ error }}
      <button type="button" @click="load">重试</button>
    </div>

    <div v-if="loading" class="adm-loading">正在读取订单…</div>

    <div v-else-if="rows.length === 0" class="adm-empty">
      <b>这个筛选下没有订单</b>
      去商城下一单，或换个状态看看
    </div>

    <div v-else class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th>订单</th>
            <th>金额</th>
            <th>件数</th>
            <th>状态</th>
            <th>下单时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.orderNo">
            <td>
              <div class="adm-prod">
                <img v-if="row.coverImage" :src="row.coverImage" :alt="row.firstItemName" />
                <div>
                  <b>{{ row.firstItemName || '订单 ' + row.orderNo }}</b>
                  <span>{{ row.orderNo }}</span>
                </div>
              </div>
            </td>
            <td style="font-weight: 600; white-space: nowrap">{{ money(row.payAmount) }}</td>
            <td style="color: var(--owl-text-3)">{{ row.totalQty }} 件</td>
            <td><span :class="['adm-tag', statusTag(row.status)]">{{ row.statusText }}</span></td>
            <td style="font-size: 12px; color: var(--owl-text-3); white-space: nowrap">
              {{ stamp(row.createdAt) }}
            </td>
            <td>
              <div style="display: flex; gap: 2px; flex-wrap: wrap">
                <button class="adm-btn link" type="button" @click="openDetail(row)">详情</button>
                <button
                  v-if="row.status === 20"
                  class="adm-btn link"
                  type="button"
                  style="color: var(--owl-red-deep)"
                  @click="openDeliver(row)"
                >
                  发货
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="!loading && rows.length > 0" class="adm-pager">
      共 {{ total }} 单 · 第 {{ page }}/{{ pages }} 页
      <span v-if="pendingCount" class="adm-tag warn" style="margin-left: 8px">
        本页 {{ pendingCount }} 单待发货
      </span>
      <span class="adm-spacer" />
      <button class="adm-btn sm" type="button" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
      <button class="adm-btn sm" type="button" :disabled="page >= pages" @click="go(page + 1)">
        下一页
      </button>
    </div>
  </section>

  <!-- 订单详情 -->
  <template v-if="detail.open">
    <div class="adm-mask" @click="detail.open = false" />
    <aside class="adm-drawer">
      <div class="adm-drawer-head">
        <h2>订单详情</h2>
        <button class="adm-btn sm" type="button" @click="detail.open = false">关闭</button>
      </div>

      <div class="adm-drawer-body">
        <div v-if="detail.loading" class="adm-loading">读取中…</div>
        <div v-else-if="detail.error" class="adm-alert">{{ detail.error }}</div>

        <template v-else-if="detail.data">
          <div class="adm-kv">
            <div><span>订单号</span><b>{{ detail.data.orderNo }}</b></div>
            <div>
              <span>状态</span>
              <b><span :class="['adm-tag', statusTag(detail.data.status)]">{{ detail.data.statusText }}</span></b>
            </div>
            <div><span>下单时间</span><b>{{ stamp(detail.data.createdAt) }}</b></div>
            <div><span>支付时间</span><b>{{ stamp(detail.data.payTime) }}</b></div>
            <div><span>配送方式</span><b>{{ detail.data.deliveryText || '快递' }}</b></div>
            <div><span>发货时间</span><b>{{ stamp(detail.data.deliverTime) }}</b></div>
          </div>

          <h3 class="adm-sub">商品</h3>
          <div class="adm-line" v-for="item in detail.data.items" :key="item.id">
            <img v-if="item.image" :src="item.image" :alt="item.name" />
            <div class="adm-line-main">
              <b>{{ item.name }}</b>
              <span>{{ item.spec }} · ×{{ item.qty }}</span>
            </div>
            <b class="adm-line-price">{{ money(item.total) }}</b>
          </div>

          <h3 class="adm-sub">金额</h3>
          <div class="adm-kv">
            <div><span>商品合计</span><b>{{ money(detail.data.amounts.goodsAmount) }}</b></div>
            <div><span>优惠</span><b>-{{ money(detail.data.amounts.discount) }}</b></div>
            <div><span>运费</span><b>{{ money(detail.data.amounts.shipFee) }}</b></div>
            <div><span>实付</span><b style="color: var(--owl-red-deep)">{{ money(detail.data.amounts.payable) }}</b></div>
          </div>

          <template v-if="detail.data.address">
            <h3 class="adm-sub">收货信息</h3>
            <div class="adm-note">
              <b>{{ detail.data.address.receiver }}</b> · {{ detail.data.address.phone }}<br />
              {{ detail.data.address.province }}{{ detail.data.address.city }}{{ detail.data.address.district }}{{ detail.data.address.detail }}
            </div>
          </template>

          <template v-if="detail.data.remark">
            <h3 class="adm-sub">买家备注</h3>
            <div class="adm-note">{{ detail.data.remark }}</div>
          </template>

          <h3 class="adm-sub">流转记录</h3>
          <ul class="adm-timeline">
            <li v-for="(node, index) in detail.data.timeline" :key="index">
              <b>{{ node.statusText || '—' }}</b>
              <span>{{ stamp(node.at) }}</span>
              <em v-if="node.remark">{{ node.remark }}</em>
            </li>
          </ul>
        </template>
      </div>

      <div class="adm-drawer-foot">
        <button class="adm-btn" type="button" @click="detail.open = false">关闭</button>
        <button
          v-if="detail.data && detail.data.status === 20"
          class="adm-btn primary"
          type="button"
          @click="openDeliver(detail.data)"
        >
          发货
        </button>
      </div>
    </aside>
  </template>

  <!-- 发货 -->
  <template v-if="deliverDlg.open">
    <div class="adm-mask" @click="deliverDlg.open = false" />
    <div class="adm-dialog">
      <h2>发货 · {{ deliverDlg.orderNo }}</h2>
      <div v-if="deliverDlg.error" class="adm-alert">{{ deliverDlg.error }}</div>

      <div class="adm-field">
        <label for="d-company">快递公司</label>
        <input id="d-company" v-model="deliverDlg.company" class="adm-input" placeholder="顺丰" />
      </div>
      <div class="adm-field">
        <label for="d-no">运单号<i>*</i></label>
        <input id="d-no" v-model="deliverDlg.trackingNo" class="adm-input" placeholder="SF1234567890" />
      </div>

      <div class="adm-dialog-foot">
        <button class="adm-btn" type="button" @click="deliverDlg.open = false">取消</button>
        <button class="adm-btn primary" type="button" :disabled="deliverDlg.pending" @click="submitDeliver">
          {{ deliverDlg.pending ? '提交中…' : '确认发货' }}
        </button>
      </div>
    </div>
  </template>

  <div v-if="message" class="adm-toast" :class="{ 'is-error': isError }">{{ message }}</div>
</template>
