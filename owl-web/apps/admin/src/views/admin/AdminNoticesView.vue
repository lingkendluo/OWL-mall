<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi, NOTICE_TYPES, type Notice, type NoticePayload, type NoticeType } from '@/services/admin'
import { ApiError } from '@shared/http'
import { useAdminToast } from './useAdminToast'
import './admin.css'

/**
 * 公告管理。状态机：草稿(0) → 已发布(1) → 已下线(2)，
 * 下线后还能再发布，发布时间保持第一次的，方便排时间线。
 */
const { message, isError, show } = useAdminToast()

const rows = ref<Notice[]>([])
const keyword = ref('')
const statusTab = ref<'all' | 'draft' | 'published' | 'offline'>('all')
const loading = ref(false)
const error = ref('')

const drawer = reactive({
  open: false,
  mode: 'create' as 'create' | 'edit',
  id: '',
  pending: false,
  error: '',
})

const form = reactive({
  title: '',
  body: '',
  type: 1 as NoticeType,
  pinned: false,
  publishNow: true,
})

const statusMap: Record<string, number | undefined> = {
  all: undefined,
  draft: 0,
  published: 1,
  offline: 2,
}

const draftCount = computed(() => rows.value.filter((item) => item.status === 0).length)

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await adminApi.notices({
      keyword: keyword.value.trim() || undefined,
      status: statusMap[statusTab.value],
    })
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载公告失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

function pickTab(tab: 'all' | 'draft' | 'published' | 'offline') {
  statusTab.value = tab
  void load()
}

function openCreate() {
  form.title = ''
  form.body = ''
  form.type = 1
  form.pinned = false
  form.publishNow = true
  drawer.mode = 'create'
  drawer.id = ''
  drawer.error = ''
  drawer.open = true
}

function openEdit(row: Notice) {
  form.title = row.title
  form.body = row.body
  form.type = row.type
  form.pinned = row.pinned
  form.publishNow = row.status === 1
  drawer.mode = 'edit'
  drawer.id = row.id
  drawer.error = ''
  drawer.open = true
}

async function submit() {
  if (drawer.pending) return
  drawer.error = ''
  if (!form.title.trim()) {
    drawer.error = '标题不能为空'
    return
  }
  if (!form.body.trim()) {
    drawer.error = '正文不能为空'
    return
  }
  const body: NoticePayload = {
    title: form.title.trim(),
    body: form.body.trim(),
    type: form.type,
    pinned: form.pinned,
    publishNow: form.publishNow,
  }
  drawer.pending = true
  try {
    if (drawer.mode === 'create') {
      await adminApi.createNotice(body)
      show(form.publishNow ? '已发布' : '已存为草稿')
    } else {
      await adminApi.updateNotice(drawer.id, body)
      show('已保存')
    }
    drawer.open = false
    await load()
  } catch (e) {
    drawer.error = e instanceof ApiError ? e.message : '提交失败'
  } finally {
    drawer.pending = false
  }
}

async function publish(row: Notice) {
  try {
    await adminApi.publishNotice(row.id)
    show('已发布，用户端立刻可见')
    await load()
  } catch (e) {
    show(e instanceof ApiError ? e.message : '发布失败', true)
  }
}

async function offline(row: Notice) {
  try {
    await adminApi.offlineNotice(row.id)
    show('已下线')
    await load()
  } catch (e) {
    show(e instanceof ApiError ? e.message : '下线失败', true)
  }
}

async function remove(row: Notice) {
  if (!window.confirm(`确定删除公告「${row.title}」？`)) return
  try {
    await adminApi.deleteNotice(row.id)
    show('已删除')
    await load()
  } catch (e) {
    show(e instanceof ApiError ? e.message : '删除失败', true)
  }
}

function statusTag(row: Notice): string {
  if (row.status === 1) return 'ok'
  if (row.status === 2) return 'off'
  return 'warn'
}
</script>

<template>
  <section class="adm-card">
    <div class="adm-toolbar">
      <div class="adm-tabs">
        <button
          v-for="tab in [
            { key: 'all', label: '全部' },
            { key: 'published', label: '已发布' },
            { key: 'draft', label: '草稿' },
            { key: 'offline', label: '已下线' },
          ]"
          :key="tab.key"
          class="adm-tab"
          :class="{ 'is-active': statusTab === tab.key }"
          type="button"
          @click="pickTab(tab.key as 'all' | 'draft' | 'published' | 'offline')"
        >
          {{ tab.label }}
        </button>
      </div>

      <input
        v-model="keyword"
        class="adm-input is-search"
        placeholder="搜公告标题，回车查询"
        @keyup.enter="load"
      />
      <button class="adm-btn sm" type="button" @click="load">查询</button>

      <span class="adm-spacer" />
      <button class="adm-btn primary" type="button" @click="openCreate">＋ 写公告</button>
    </div>

    <div v-if="error" class="adm-alert" style="margin: 12px 16px">
      {{ error }}
      <button type="button" @click="load">重试</button>
    </div>

    <div v-if="loading" class="adm-loading">正在读取公告…</div>

    <div v-else-if="rows.length === 0" class="adm-empty">
      <b>还没有公告</b>
      点右上角「写公告」，写完可以直接发布
    </div>

    <div v-else class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th>公告</th>
            <th>类型</th>
            <th>状态</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td style="max-width: 460px">
              <b style="display: block; font-weight: 600; font-size: 13px">
                <span v-if="row.pinned" class="adm-tag warn" style="margin-right: 6px">置顶</span>
                {{ row.title }}
              </b>
              <span
                style="
                  display: block;
                  margin-top: 3px;
                  font-size: 11.5px;
                  color: var(--owl-text-3);
                  overflow: hidden;
                  text-overflow: ellipsis;
                  white-space: nowrap;
                "
              >
                {{ row.body }}
              </span>
            </td>
            <td><span class="adm-tag info">{{ row.typeText }}</span></td>
            <td><span :class="['adm-tag', statusTag(row)]">{{ row.statusText }}</span></td>
            <td style="font-size: 12px; color: var(--owl-text-3); white-space: nowrap">{{ row.time }}</td>
            <td>
              <div style="display: flex; gap: 2px; flex-wrap: wrap">
                <button class="adm-btn link" type="button" @click="openEdit(row)">编辑</button>
                <button v-if="row.status !== 1" class="adm-btn link" type="button" @click="publish(row)">
                  发布
                </button>
                <button v-else class="adm-btn link" type="button" @click="offline(row)">下线</button>
                <button
                  class="adm-btn link"
                  type="button"
                  style="color: var(--owl-red-deep)"
                  @click="remove(row)"
                >
                  删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="!loading && rows.length > 0" class="adm-pager">
      共 {{ rows.length }} 条<template v-if="draftCount"> · 其中草稿 {{ draftCount }} 条</template>
    </div>
  </section>

  <template v-if="drawer.open">
    <div class="adm-mask" @click="drawer.open = false" />
    <aside class="adm-drawer">
      <div class="adm-drawer-head">
        <h2>{{ drawer.mode === 'create' ? '写公告' : '编辑公告' }}</h2>
        <button class="adm-btn sm" type="button" @click="drawer.open = false">关闭</button>
      </div>

      <div class="adm-drawer-body">
        <div v-if="drawer.error" class="adm-alert">{{ drawer.error }}</div>

        <div class="adm-field">
          <label for="n-title">标题<i>*</i></label>
          <input id="n-title" v-model="form.title" class="adm-input" maxlength="120" placeholder="如：双十一预热，满 199 减 30" />
        </div>

        <div class="adm-field">
          <label for="n-type">类型</label>
          <select id="n-type" v-model="form.type" class="adm-select">
            <option v-for="item in NOTICE_TYPES" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </div>

        <div class="adm-field">
          <label for="n-body">正文<i>*</i></label>
          <textarea
            id="n-body"
            v-model="form.body"
            class="adm-textarea"
            style="min-height: 180px"
            placeholder="写清楚时间、范围、注意事项"
          />
        </div>

        <label class="adm-check">
          <input v-model="form.pinned" type="checkbox" />
          置顶（首页优先展示）
        </label>

        <label class="adm-check">
          <input v-model="form.publishNow" type="checkbox" />
          保存后立即发布（不勾则存为草稿）
        </label>
      </div>

      <div class="adm-drawer-foot">
        <button class="adm-btn" type="button" @click="drawer.open = false">取消</button>
        <button class="adm-btn primary" type="button" :disabled="drawer.pending" @click="submit">
          {{ drawer.pending ? '提交中…' : drawer.mode === 'create' ? '保存' : '保存修改' }}
        </button>
      </div>
    </aside>
  </template>

  <div v-if="message" class="adm-toast" :class="{ 'is-error': isError }">{{ message }}</div>
</template>
