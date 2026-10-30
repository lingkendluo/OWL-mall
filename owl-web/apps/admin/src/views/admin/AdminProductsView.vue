<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  adminApi,
  type AdminProduct,
  type Category,
  type CategoryStat,
  type ProductPayload,
  type StockLog,
} from '@/services/admin'
import { ApiError } from '@shared/http'
import { useAdminToast } from './useAdminToast'
import './admin.css'

/**
 * 商品管理：列表 + 上架抽屉 + 库存调整。
 * 价格用「元」录入、元存储（后端 AdminProductRequest 就是 BigDecimal 元），
 * 库里 c_product 存的是分，换算在 ProductAdminService 里做，前端不用管。
 */
const { message, isError, show } = useAdminToast()

const rows = ref<AdminProduct[]>([])
const total = ref(0)
const pages = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')
const statusTab = ref<'all' | 'on' | 'off'>('all')
const loading = ref(false)
const error = ref('')
const categories = ref<Category[]>([])

/** 顶部大类索引：每类的总数与在售/下架拆分 */
const stats = ref<CategoryStat[]>([])
/** 当前选中的大类 code，空串表示「全部」 */
const activeCategory = ref('')

/** 索引条上的数字跟着「全部/在售/已下架」这三个 tab 变，避免两边口径不一致 */
function pickCount(stat: CategoryStat): number {
  if (statusTab.value === 'on') return stat.onSale
  if (statusTab.value === 'off') return stat.offSale
  return stat.total
}

/** 「全部」那个 chip 的数字就是各分类之和 */
const statTotal = computed(() => stats.value.reduce((sum, item) => sum + pickCount(item), 0))

/** 正在切换状态的商品，防止连点发多次请求（reactive Set 的增删会触发视图更新） */
const toggling = reactive(new Set<string>())

const drawer = reactive({
  open: false,
  mode: 'create' as 'create' | 'edit',
  code: '',
  pending: false,
  error: '',
})

const blank = {
  name: '',
  categoryCode: '',
  subCode: '',
  spec: '',
  price: '',
  list: '',
  tag: '',
  emoji: '',
  image: '',
  stock: '100',
  isFeatured: false,
  slogan: '',
  intro: '',
  tips: '',
}

const form = reactive({ ...blank })

const stockDlg = reactive({
  open: false,
  code: '',
  name: '',
  current: 0,
  delta: '10',
  reason: '补货',
  pending: false,
})

const subOptions = computed(
  () => categories.value.find((item) => item.id === form.categoryCode)?.subs ?? [],
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await adminApi.products({
      keyword: keyword.value.trim() || undefined,
      status: statusTab.value === 'all' ? undefined : statusTab.value === 'on' ? 1 : 0,
      category: activeCategory.value || undefined,
      page: page.value,
      size: pageSize,
    })
    rows.value = result.list
    total.value = result.total
    pages.value = result.pages
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载商品失败'
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await adminApi.categories()
  } catch {
    /* 类目加载失败不阻塞列表，上架时会提示 */
  }
}

async function loadStats() {
  try {
    stats.value = await adminApi.productStats()
  } catch {
    /* 统计失败只是索引条没数字，不影响列表本身 */
    stats.value = []
  }
}

/** 点大类 chip 过滤；再点一次取消，回到「全部」 */
function pickCategory(code: string) {
  activeCategory.value = activeCategory.value === code ? '' : code
  page.value = 1
  void load()
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadStats(), load()])
})

watch(statusTab, () => {
  page.value = 1
  void load()
})

function search() {
  page.value = 1
  void load()
}

function turn(delta: number) {
  const next = page.value + delta
  if (next < 1 || (pages.value && next > pages.value)) return
  page.value = next
  void load()
}

/* ------------------------------ 抽屉 ------------------------------ */

function resetForm() {
  Object.assign(form, blank)
  const first = categories.value[0]
  if (first) {
    form.categoryCode = first.id
    form.subCode = first.subs[0]?.id ?? ''
  }
}

function openCreate() {
  if (categories.value.length === 0) {
    show('类目还没读到，请确认后端已启动', true)
    return
  }
  resetForm()
  drawer.mode = 'create'
  drawer.code = ''
  drawer.error = ''
  drawer.open = true
}

function openEdit(row: AdminProduct) {
  Object.assign(form, {
    name: row.name,
    categoryCode: row.categoryCode,
    subCode: row.subCode,
    spec: row.spec ?? '',
    price: String(row.price),
    list: String(row.list),
    tag: row.tag ?? '',
    emoji: row.emoji ?? '',
    image: row.image ?? '',
    stock: String(row.stock),
    isFeatured: row.isFeatured,
    slogan: row.detail?.slogan ?? '',
    intro: row.detail?.intro ?? '',
    tips: (row.detail?.tips ?? []).join('\n'),
  })
  drawer.mode = 'edit'
  drawer.code = row.id
  drawer.error = ''
  drawer.open = true
}

function onCategoryChange() {
  form.subCode = subOptions.value[0]?.id ?? ''
}

function payload(): ProductPayload | string {
  const price = Number(form.price)
  const listPrice = Number(form.list)
  if (!form.name.trim()) return '商品名不能为空'
  if (!form.categoryCode) return '请选择类目'
  if (!form.subCode) return '请选择细分'
  if (!Number.isFinite(price) || price <= 0) return '售价必须是大于 0 的数字'
  if (!Number.isFinite(listPrice) || listPrice <= 0) return '划线价必须是大于 0 的数字'
  if (listPrice < price) return '划线价不能低于售价'

  return {
    name: form.name.trim(),
    categoryCode: form.categoryCode,
    subCode: form.subCode,
    spec: form.spec.trim(),
    price,
    list: listPrice,
    tag: form.tag.trim(),
    emoji: form.emoji.trim() || '🛍️',
    image: form.image.trim() || undefined,
    stock: Number(form.stock) || 0,
    isFeatured: form.isFeatured,
    detail: {
      slogan: form.slogan.trim() || undefined,
      intro: form.intro.trim() || undefined,
      tips: form.tips
        .split('\n')
        .map((line) => line.trim())
        .filter(Boolean),
    },
  }
}

async function submit() {
  if (drawer.pending) return
  drawer.error = ''
  const body = payload()
  if (typeof body === 'string') {
    drawer.error = body
    return
  }
  drawer.pending = true
  try {
    if (drawer.mode === 'create') {
      const created = await adminApi.createProduct(body)
      show(`已上架：${created.name}`)
    } else {
      await adminApi.updateProduct(drawer.code, body)
      show('已保存修改')
    }
    drawer.open = false
    await Promise.all([load(), loadStats()])
  } catch (e) {
    drawer.error = e instanceof ApiError ? e.message : '提交失败'
  } finally {
    drawer.pending = false
  }
}

/* ------------------------------ 行内操作 ------------------------------ */

async function toggleStatus(row: AdminProduct) {
  const next = row.status === 1 ? 0 : 1
  // 双击时别发两次请求，状态会来回跳
  if (toggling.has(row.id)) return
  toggling.add(row.id)
  try {
    const updated = await adminApi.changeProductStatus(row.id, next as 0 | 1)
    row.status = updated.status
    show(updated.status === 1 ? '已上架，用户端立刻可见' : '已下架，用户端不再展示')
    void loadStats()
  } catch (e) {
    show(e instanceof ApiError ? e.message : '操作失败', true)
  } finally {
    toggling.delete(row.id)
  }
}

function openStock(row: AdminProduct) {
  stockDlg.open = true
  stockDlg.code = row.id
  stockDlg.name = row.name
  stockDlg.current = row.stock
  stockDlg.delta = '10'
  stockDlg.reason = '补货'
  stockDlg.pending = false
}

async function submitStock() {
  const delta = Number(stockDlg.delta)
  if (!Number.isFinite(delta) || delta === 0) {
    show('调整数量不能为 0', true)
    return
  }
  stockDlg.pending = true
  try {
    const updated = await adminApi.adjustStock(stockDlg.code, Math.trunc(delta), stockDlg.reason.trim() || undefined)
    stockDlg.open = false
    show(`库存已更新为 ${updated.stock}`)
    await Promise.all([load(), loadStats()])
  } catch (e) {
    show(e instanceof ApiError ? e.message : '调整失败', true)
  } finally {
    stockDlg.pending = false
  }
}

/* --------------------------- 库存流水 --------------------------- */

const flowDlg = reactive({
  open: false,
  code: '',
  name: '',
  loading: false,
  error: '',
  rows: [] as StockLog[],
})

async function openFlow(row: AdminProduct) {
  flowDlg.open = true
  flowDlg.code = row.id
  flowDlg.name = row.name
  flowDlg.rows = []
  flowDlg.error = ''
  flowDlg.loading = true
  try {
    const result = await adminApi.stockLogs(row.id, { page: 1, size: 50 })
    flowDlg.rows = result.list
  } catch (e) {
    flowDlg.error = e instanceof ApiError ? e.message : '读取流水失败'
  } finally {
    flowDlg.loading = false
  }
}

async function remove(row: AdminProduct) {
  if (!window.confirm(`确定删除「${row.name}」？删除后首页和分类页都不会再出现这件商品。`)) return
  try {
    await adminApi.deleteProduct(row.id)
    show('已删除')
    if (rows.value.length === 1 && page.value > 1) page.value -= 1
    await Promise.all([load(), loadStats()])
  } catch (e) {
    show(e instanceof ApiError ? e.message : '删除失败', true)
  }
}

function categoryName(code: string): string {
  return categories.value.find((item) => item.id === code)?.name ?? code
}

/** 细分列显示中文名，类目还没读到的时候退回 code，不留空白 */
function subName(row: AdminProduct): string {
  const cat = categories.value.find((item) => item.id === row.categoryCode)
  return cat?.subs.find((sub) => sub.id === row.subCode)?.name ?? row.subCode
}
</script>

<template>
  <section class="adm-card">
    <div class="adm-toolbar">
      <div class="adm-tabs">
        <button
          v-for="tab in [
            { key: 'all', label: '全部' },
            { key: 'on', label: '在售' },
            { key: 'off', label: '已下架' },
          ]"
          :key="tab.key"
          class="adm-tab"
          :class="{ 'is-active': statusTab === tab.key }"
          type="button"
          @click="statusTab = tab.key as 'all' | 'on' | 'off'"
        >
          {{ tab.label }}
        </button>
      </div>

      <input
        v-model="keyword"
        class="adm-input is-search"
        placeholder="搜商品名，回车查询"
        @keyup.enter="search"
      />
      <button class="adm-btn sm" type="button" @click="search">查询</button>

      <span class="adm-spacer" />
      <button class="adm-btn primary" type="button" @click="openCreate">＋ 上架新商品</button>
    </div>

    <!-- 大类索引：点一下按大类过滤，再点一次取消。数字跟当前「全部/在售/已下架」tab 走 -->
    <div v-if="stats.length" class="adm-cats">
      <button
        class="adm-cat"
        :class="{ 'is-active': !activeCategory }"
        type="button"
        @click="activeCategory = ''; page = 1; void load()"
      >
        全部<em>{{ statTotal }}</em>
      </button>
      <button
        v-for="stat in stats"
        :key="stat.code"
        class="adm-cat"
        :class="{ 'is-active': activeCategory === stat.code }"
        type="button"
        :disabled="pickCount(stat) === 0"
        @click="pickCategory(stat.code)"
      >
        {{ stat.name }}<em>{{ pickCount(stat) }}</em>
      </button>
    </div>

    <div v-if="error" class="adm-alert" style="margin: 12px 16px">
      {{ error }}
      <button type="button" @click="load">重试</button>
    </div>

    <div v-if="loading" class="adm-loading">正在读取商品…</div>

    <div v-else-if="rows.length === 0" class="adm-empty">
      <b>没有符合条件的商品</b>
      换个关键词，或点右上角「上架新商品」
    </div>

    <div v-else class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th>商品</th>
            <th>类目 / 细分</th>
            <th class="num">售价</th>
            <th class="num">划线价</th>
            <th class="num">库存</th>
            <th class="num">销量</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>
              <div class="adm-prod">
                <img
                  v-if="row.image"
                  :src="row.image"
                  :alt="row.name"
                  loading="lazy"
                  @error="($event.target as HTMLImageElement).style.visibility = 'hidden'"
                />
                <div>
                  <b>{{ row.name }}</b>
                  <span>{{ row.id }}<template v-if="row.spec"> · {{ row.spec }}</template></span>
                </div>
              </div>
            </td>
            <td>
              <div style="font-size: 12.5px">{{ categoryName(row.categoryCode) }}</div>
              <div style="font-size: 11px; color: var(--owl-text-3)">{{ subName(row) }}</div>
            </td>
            <td class="num">¥{{ row.price }}</td>
            <td class="num" style="color: var(--owl-text-3)">¥{{ row.list }}</td>
            <td class="num">
              <span :class="['adm-tag', row.stock < 10 ? 'warn' : 'off']">{{ row.stock }}</span>
            </td>
            <td class="num">{{ row.sales }}</td>
            <td>
              <div class="adm-status-cell">
                <button
                  class="adm-switch"
                  :class="{ 'is-on': row.status === 1, 'is-busy': toggling.has(row.id) }"
                  type="button"
                  role="switch"
                  :aria-checked="row.status === 1"
                  :aria-label="row.status === 1 ? `下架 ${row.name}` : `上架 ${row.name}`"
                  :disabled="toggling.has(row.id)"
                  @click="toggleStatus(row)"
                >
                  <i />
                </button>
                <span class="adm-status-text">{{ row.status === 1 ? '在售' : '已下架' }}</span>
              </div>
              <span v-if="row.isFeatured" class="adm-tag info" style="margin-left: 4px">推荐</span>
            </td>
            <td>
              <div style="display: flex; gap: 2px; flex-wrap: wrap">
                <button class="adm-btn link" type="button" @click="openEdit(row)">编辑</button>
                <button class="adm-btn link" type="button" @click="openStock(row)">库存</button>
                <button class="adm-btn link" type="button" @click="openFlow(row)">流水</button>
                <button class="adm-btn link" type="button" style="color: var(--owl-red-deep)" @click="remove(row)">
                  删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="!loading && rows.length > 0" class="adm-pager">
      共 {{ total }} 件 · 第 {{ page }} / {{ pages || 1 }} 页
      <button class="adm-btn sm" type="button" :disabled="page <= 1" @click="turn(-1)">上一页</button>
      <button class="adm-btn sm" type="button" :disabled="page >= pages" @click="turn(1)">下一页</button>
    </div>
  </section>

  <!-- 上架 / 编辑抽屉 -->
  <template v-if="drawer.open">
    <div class="adm-mask" @click="drawer.open = false" />
    <aside class="adm-drawer">
      <div class="adm-drawer-head">
        <h2>{{ drawer.mode === 'create' ? '上架新商品' : `编辑：${drawer.code}` }}</h2>
        <button class="adm-btn sm" type="button" @click="drawer.open = false">关闭</button>
      </div>

      <div class="adm-drawer-body">
        <div v-if="drawer.error" class="adm-alert">{{ drawer.error }}</div>

        <div class="adm-form-grid">
          <div class="adm-field span-2">
            <label for="p-name">商品名<i>*</i></label>
            <input id="p-name" v-model="form.name" class="adm-input" placeholder="如：乐事薯片 原味 135g" />
          </div>

          <div class="adm-field">
            <label for="p-cat">类目<i>*</i></label>
            <select id="p-cat" v-model="form.categoryCode" class="adm-select" @change="onCategoryChange">
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>

          <div class="adm-field">
            <label for="p-sub">细分<i>*</i></label>
            <select id="p-sub" v-model="form.subCode" class="adm-select">
              <option v-for="sub in subOptions" :key="sub.id" :value="sub.id">
                {{ sub.name }}（现有 {{ sub.count }}）
              </option>
            </select>
          </div>

          <div class="adm-field">
            <label for="p-price">售价（元）<i>*</i></label>
            <input id="p-price" v-model="form.price" class="adm-input" inputmode="decimal" placeholder="9.9" />
          </div>

          <div class="adm-field">
            <label for="p-list">划线价（元）<i>*</i></label>
            <input id="p-list" v-model="form.list" class="adm-input" inputmode="decimal" placeholder="12.9" />
            <span class="adm-field-hint">必须 ≥ 售价，详情页会显示「已省」</span>
          </div>

          <div class="adm-field">
            <label for="p-spec">规格</label>
            <input id="p-spec" v-model="form.spec" class="adm-input" placeholder="135g / 12 包 / 1.5L" />
          </div>

          <div class="adm-field">
            <label for="p-stock">初始库存</label>
            <input id="p-stock" v-model="form.stock" class="adm-input" inputmode="numeric" />
          </div>

          <div class="adm-field">
            <label for="p-tag">角标</label>
            <input id="p-tag" v-model="form.tag" class="adm-input" placeholder="包邮 / 第二件半价" />
          </div>

          <div class="adm-field">
            <label for="p-emoji">图标 emoji</label>
            <input id="p-emoji" v-model="form.emoji" class="adm-input" placeholder="🛍️" />
          </div>

          <div class="adm-field span-2">
            <label for="p-image">主图地址</label>
            <input
              id="p-image"
              v-model="form.image"
              class="adm-input"
              placeholder="/static/products/snacks-1.jpg"
            />
            <span class="adm-field-hint">
              留空则沿用后端默认图；要换成你自己拍的图，把 jpg 放进 owl-server 的 static/products 再填路径
            </span>
          </div>

          <div v-if="form.image" class="span-2">
            <img
              :src="form.image"
              alt="主图预览"
              style="width: 88px; height: 88px; border-radius: 10px; object-fit: cover; border: 1px solid #e6e7e9"
            />
          </div>

          <label class="adm-check span-2">
            <input v-model="form.isFeatured" type="checkbox" />
            设为推荐商品（首页「为你推荐」会带出来）
          </label>

          <div class="adm-field span-2">
            <label for="p-slogan">一句话卖点</label>
            <input id="p-slogan" v-model="form.slogan" class="adm-input" placeholder="酥脆到掉渣，一包接一包" />
          </div>

          <div class="adm-field span-2">
            <label for="p-intro">详情简介</label>
            <textarea id="p-intro" v-model="form.intro" class="adm-textarea" placeholder="给顾客看的商品说明" />
          </div>

          <div class="adm-field span-2">
            <label for="p-tips">购买提示</label>
            <textarea
              id="p-tips"
              v-model="form.tips"
              class="adm-textarea"
              style="min-height: 84px"
              placeholder="一行一条，如：冷藏保存，开封后尽快食用"
            />
          </div>
        </div>
      </div>

      <div class="adm-drawer-foot">
        <button class="adm-btn" type="button" @click="drawer.open = false">取消</button>
        <button class="adm-btn primary" type="button" :disabled="drawer.pending" @click="submit">
          {{ drawer.pending ? '提交中…' : drawer.mode === 'create' ? '确认上架' : '保存修改' }}
        </button>
      </div>
    </aside>
  </template>

  <!-- 库存调整 -->
  <template v-if="stockDlg.open">
    <div class="adm-mask" @click="stockDlg.open = false" />
    <aside class="adm-drawer" style="width: min(400px, 92vw)">
      <div class="adm-drawer-head">
        <h2>调整库存</h2>
        <button class="adm-btn sm" type="button" @click="stockDlg.open = false">关闭</button>
      </div>
      <div class="adm-drawer-body">
        <div style="font-size: 13px; line-height: 1.7">
          <b>{{ stockDlg.name }}</b><br />
          <span style="color: var(--owl-text-3)">当前库存 {{ stockDlg.current }}</span>
        </div>
        <div class="adm-field">
          <label for="s-delta">调整数量</label>
          <input id="s-delta" v-model="stockDlg.delta" class="adm-input" inputmode="numeric" />
          <span class="adm-field-hint">正数入库、负数出库，提交后记为一条库存流水</span>
        </div>
        <div class="adm-field">
          <label for="s-reason">原因</label>
          <input id="s-reason" v-model="stockDlg.reason" class="adm-input" placeholder="补货 / 盘点 / 报损" />
        </div>
      </div>
      <div class="adm-drawer-foot">
        <button class="adm-btn" type="button" @click="stockDlg.open = false">取消</button>
        <button class="adm-btn primary" type="button" :disabled="stockDlg.pending" @click="submitStock">
          {{ stockDlg.pending ? '提交中…' : '确认调整' }}
        </button>
      </div>
    </aside>
  </template>

  <!-- 库存流水 -->
  <template v-if="flowDlg.open">
    <div class="adm-mask" @click="flowDlg.open = false" />
    <aside class="adm-drawer" style="width: min(460px, 92vw)">
      <div class="adm-drawer-head">
        <h2>库存流水</h2>
        <button class="adm-btn sm" type="button" @click="flowDlg.open = false">关闭</button>
      </div>
      <div class="adm-drawer-body">
        <div style="font-size: 13px; line-height: 1.7">
          <b>{{ flowDlg.name }}</b><br />
          <span style="color: var(--owl-text-3)">{{ flowDlg.code }} · 最近 50 条</span>
        </div>

        <div v-if="flowDlg.loading" class="adm-loading">读取中…</div>
        <div v-else-if="flowDlg.error" class="adm-alert">{{ flowDlg.error }}</div>
        <div v-else-if="flowDlg.rows.length === 0" class="adm-empty" style="padding: 28px 0">
          <b>还没有流水</b>
          下单扣减、补货入库、后台调整都会记一条
        </div>

        <ul v-else class="adm-flow">
          <li v-for="log in flowDlg.rows" :key="log.id">
            <span :class="['adm-tag', log.changeQty >= 0 ? 'ok' : 'info']">
              {{ log.changeQty >= 0 ? '+' + log.changeQty : log.changeQty }}
            </span>
            <div class="adm-flow-main">
              <b>{{ log.typeText }}</b>
              <span>{{ log.beforeQty }} → {{ log.afterQty }}<template v-if="log.refNo"> · {{ log.refNo }}</template></span>
            </div>
            <span class="adm-flow-time">{{ log.time }}</span>
          </li>
        </ul>
      </div>
      <div class="adm-drawer-foot">
        <button class="adm-btn" type="button" @click="flowDlg.open = false">关闭</button>
      </div>
    </aside>
  </template>

  <div v-if="message" class="adm-toast" :class="{ 'is-error': isError }">{{ message }}</div>
</template>
