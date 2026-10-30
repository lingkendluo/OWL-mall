<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi, type AdminMember } from '@/services/admin'
import { ApiError } from '@shared/http'
import { useAdminToast } from './useAdminToast'
import './admin.css'

/**
 * 会员管理。只做「看 + 启用/禁用」，改等级和积分属于运营动作，
 * 需要走单独的接口，这版先不放出来，避免误操作改坏账户。
 */
const { message, isError, show } = useAdminToast()

const TABS: Array<{ key: string; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'on', label: '正常' },
  { key: 'off', label: '已禁用' },
]

const rows = ref<AdminMember[]>([])
const total = ref(0)
const pages = ref(1)
const page = ref(1)
const pageSize = 12
const statusTab = ref('all')
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const busy = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await adminApi.members({
      keyword: keyword.value.trim() || undefined,
      status: statusTab.value === 'on' ? 1 : statusTab.value === 'off' ? 0 : undefined,
      page: page.value,
      size: pageSize,
    })
    rows.value = result.list
    total.value = result.total
    pages.value = result.pages
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载会员失败'
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)

function pickTab(tab: string) {
  statusTab.value = tab
  page.value = 1
  void load()
}

function go(next: number) {
  if (next < 1 || next > pages.value) return
  page.value = next
  void load()
}

function stamp(at: number | null): string {
  if (!at) return '—'
  const date = new Date(at)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

async function toggle(row: AdminMember) {
  const next = row.status === 1 ? 0 : 1
  if (next === 0 && !window.confirm(`禁用「${row.nickname || row.phone}」后该账号将无法登录，确定？`)) {
    return
  }
  busy.value = row.id
  try {
    const updated = await adminApi.changeMemberStatus(row.id, next as 0 | 1)
    show(updated.status === 1 ? '已恢复' : '已禁用')
    await load()
  } catch (e) {
    show(e instanceof ApiError ? e.message : '操作失败', true)
  } finally {
    busy.value = ''
  }
}
</script>

<template>
  <section class="adm-card">
    <div class="adm-toolbar">
      <div class="adm-tabs">
        <button
          v-for="tab in TABS"
          :key="tab.key"
          class="adm-tab"
          :class="{ 'is-active': statusTab === tab.key }"
          type="button"
          @click="pickTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <input
        v-model="keyword"
        class="adm-input is-search"
        placeholder="搜昵称 / 手机号 / 会员号，回车查询"
        @keyup.enter="page = 1; load()"
      />
      <button class="adm-btn sm" type="button" @click="page = 1; load()">查询</button>
    </div>

    <div v-if="error" class="adm-alert" style="margin: 12px 16px">
      {{ error }}
      <button type="button" @click="load">重试</button>
    </div>

    <div v-if="loading" class="adm-loading">正在读取会员…</div>

    <div v-else-if="rows.length === 0" class="adm-empty">
      <b>还没有会员</b>
      用户注册后会自动出现在这里
    </div>

    <div v-else class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th>会员</th>
            <th>等级</th>
            <th>积分</th>
            <th>余额</th>
            <th>注册</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>
              <div class="adm-prod" style="min-width: 200px">
                <div>
                  <b>
                    {{ row.nickname || '未命名' }}
                    <span v-if="row.guest" class="adm-tag" style="margin-left: 6px">访客</span>
                  </b>
                  <span>{{ row.phone || row.email || '未绑定手机' }}</span>
                </div>
              </div>
            </td>
            <td>
              <span :class="['adm-tag', row.level === 'black' ? 'warn' : 'info']">{{ row.levelText }}</span>
            </td>
            <td style="font-weight: 600">{{ row.points }}</td>
            <td style="font-weight: 600">¥{{ row.balance.toFixed(2) }}</td>
            <td style="font-size: 12px; color: var(--owl-text-3); white-space: nowrap">
              {{ stamp(row.createdAt) }}
            </td>
            <td>
              <span :class="['adm-tag', row.status === 1 ? 'ok' : 'off']">
                {{ row.status === 1 ? '正常' : '已禁用' }}
              </span>
            </td>
            <td>
              <button
                class="adm-btn link"
                type="button"
                :disabled="busy === row.id"
                :style="row.status === 1 ? 'color: var(--owl-red-deep)' : ''"
                @click="toggle(row)"
              >
                {{ busy === row.id ? '处理中…' : row.status === 1 ? '禁用' : '恢复' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="!loading && rows.length > 0" class="adm-pager">
      共 {{ total }} 位会员 · 第 {{ page }}/{{ pages }} 页
      <span class="adm-spacer" />
      <button class="adm-btn sm" type="button" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
      <button class="adm-btn sm" type="button" :disabled="page >= pages" @click="go(page + 1)">
        下一页
      </button>
    </div>
  </section>

  <div v-if="message" class="adm-toast" :class="{ 'is-error': isError }">{{ message }}</div>
</template>
