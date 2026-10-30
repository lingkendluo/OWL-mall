<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi, type AdminLog } from '@/services/admin'
import { ApiError } from '@shared/http'
import './admin.css'

/**
 * 操作日志。数据来自 @AdminAction 切面，只记录写操作，
 * 读接口不落库，不然一天能刷几万条。
 */
const ACTION_TEXT: Record<string, string> = {
  PRODUCT_CREATE: '上架商品',
  PRODUCT_UPDATE: '修改商品',
  PRODUCT_DELETE: '删除商品',
  PRODUCT_STATUS: '上下架',
  PRODUCT_STOCK: '调库存',
  CATEGORY_CREATE: '新建类目',
  CATEGORY_UPDATE: '改类目',
  NOTICE_CREATE: '写公告',
  NOTICE_UPDATE: '改公告',
  NOTICE_PUBLISH: '发布公告',
  NOTICE_OFFLINE: '下线公告',
  NOTICE_DELETE: '删公告',
  ORDER_DELIVER: '订单发货',
  MEMBER_STATUS: '会员启停',
}

const ACTIONS = Object.keys(ACTION_TEXT)

const rows = ref<AdminLog[]>([])
const total = ref(0)
const pages = ref(1)
const page = ref(1)
const pageSize = 20
const keyword = ref('')
const action = ref('')
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await adminApi.logs({
      keyword: keyword.value.trim() || undefined,
      action: action.value || undefined,
      page: page.value,
      size: pageSize,
    })
    rows.value = result.list
    total.value = result.total
    pages.value = result.pages
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载日志失败'
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function search() {
  page.value = 1
  void load()
}

function go(next: number) {
  if (next < 1 || next > pages.value) return
  page.value = next
  void load()
}

function label(code: string): string {
  return ACTION_TEXT[code] ?? code
}

function actionTag(code: string): string {
  if (code.endsWith('_DELETE')) return 'off'
  if (code.endsWith('_CREATE') || code.endsWith('_PUBLISH') || code === 'ORDER_DELIVER') return 'ok'
  if (code === 'PRODUCT_STOCK' || code === 'MEMBER_STATUS') return 'warn'
  return 'info'
}

function prettyDetail(raw: string): string {
  if (!raw) return ''
  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed)
        .map(([key, value]) => `${key}=${value === null ? 'null' : String(value)}`)
        .join(' · ')
    }
  } catch {
    // 不是 JSON 就原样展示
  }
  return raw
}
</script>

<template>
  <section class="adm-card">
    <div class="adm-toolbar">
      <select v-model="action" class="adm-select" style="width: 150px" @change="search">
        <option value="">全部动作</option>
        <option v-for="code in ACTIONS" :key="code" :value="code">{{ label(code) }}</option>
      </select>

      <input
        v-model="keyword"
        class="adm-input is-search"
        placeholder="搜对象 / 明细，回车查询"
        @keyup.enter="search"
      />
      <button class="adm-btn sm" type="button" @click="search">查询</button>

      <span class="adm-spacer" />
      <button class="adm-btn sm" type="button" @click="load">刷新</button>
    </div>

    <div v-if="error" class="adm-alert" style="margin: 12px 16px">
      {{ error }}
      <button type="button" @click="load">重试</button>
    </div>

    <div v-if="loading" class="adm-loading">正在读取日志…</div>

    <div v-else-if="rows.length === 0" class="adm-empty">
      <b>还没有操作记录</b>
      每次上架商品、调价、发货都会记在这里
    </div>

    <div v-else class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th>动作</th>
            <th>操作人</th>
            <th>对象</th>
            <th>明细</th>
            <th>IP</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td><span :class="['adm-tag', actionTag(row.action)]">{{ label(row.action) }}</span></td>
            <td style="font-weight: 600; white-space: nowrap">{{ row.adminName || '—' }}</td>
            <td style="font-size: 12.5px; white-space: nowrap">{{ row.target || '—' }}</td>
            <td style="max-width: 380px">
              <span
                style="
                  display: block;
                  font-size: 12px;
                  color: var(--owl-text-3);
                  overflow: hidden;
                  text-overflow: ellipsis;
                  white-space: nowrap;
                "
                :title="row.detail"
              >
                {{ prettyDetail(row.detail) || '—' }}
              </span>
            </td>
            <td style="font-size: 12px; color: var(--owl-text-3); white-space: nowrap">{{ row.ip || '—' }}</td>
            <td style="font-size: 12px; color: var(--owl-text-3); white-space: nowrap">{{ row.time }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="!loading && rows.length > 0" class="adm-pager">
      共 {{ total }} 条 · 第 {{ page }}/{{ pages }} 页
      <span class="adm-spacer" />
      <button class="adm-btn sm" type="button" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
      <button class="adm-btn sm" type="button" :disabled="page >= pages" @click="go(page + 1)">
        下一页
      </button>
    </div>
  </section>
</template>
