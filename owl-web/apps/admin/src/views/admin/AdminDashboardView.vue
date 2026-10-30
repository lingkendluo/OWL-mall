<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { adminApi, type Notice, type Stats } from '@/services/admin'
import { ApiError } from '@shared/http'
import { useAdminToast } from './useAdminToast'
import './admin.css'

/**
 * 概览页。只读统计，写操作都在商品/公告页，避免这里变成一个什么都点得到的面板。
 */
const stats = ref<Stats | null>(null)
const notices = ref<Notice[]>([])
const loading = ref(true)
const error = ref('')
const { message, isError, show } = useAdminToast()

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [s, n] = await Promise.all([
      adminApi.stats(),
      adminApi.notices().catch(() => [] as Notice[]),
    ])
    stats.value = s
    notices.value = n.filter((item) => item.status === 1).slice(0, 3)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

function yuan(value: number | undefined | null): string {
  if (value === undefined || value === null) return '—'
  return `¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

async function offline(id: string) {
  try {
    await adminApi.offlineNotice(id)
    show('已下线')
    await load()
  } catch (e) {
    show(e instanceof ApiError ? e.message : '操作失败', true)
  }
}
</script>

<template>
  <div v-if="error" class="adm-alert">
    {{ error }}
    <button type="button" @click="load">重试</button>
  </div>

  <div v-if="loading" class="adm-loading">正在读取经营数据…</div>

  <template v-else-if="stats">
    <section class="adm-stats">
      <div class="adm-stat">
        <span>累计成交额</span>
        <b>{{ yuan(stats.gmv) }}</b>
      </div>
      <div class="adm-stat">
        <span>今日成交额</span>
        <b>{{ yuan(stats.todayGmv) }}</b>
      </div>
      <div class="adm-stat">
        <span>今日订单</span>
        <b>{{ stats.todayOrderCount }}<em>单</em></b>
      </div>
      <div class="adm-stat">
        <span>累计订单</span>
        <b>{{ stats.orderCount }}<em>单</em></b>
      </div>
      <div class="adm-stat">
        <span>商品数量</span>
        <b>{{ stats.productCount }}<em>件</em></b>
      </div>
      <div class="adm-stat" :class="{ 'is-alert': stats.lowStockCount > 0 }">
        <span>库存告警（&lt; 10）</span>
        <b>{{ stats.lowStockCount }}<em>件</em></b>
      </div>
    </section>

    <div class="adm-form-grid">
      <section class="adm-card">
        <div class="adm-card-head">
          <div>
            <h2>待办</h2>
            <p>库存见底的商品优先处理，避免下单后无法发货</p>
          </div>
          <RouterLink :to="{ name: 'admin-products' }" class="adm-btn sm">去商品页</RouterLink>
        </div>
        <div class="adm-card-body" style="font-size: 13px; line-height: 1.9">
          <p v-if="stats.lowStockCount > 0" style="margin: 0">
            有 <b>{{ stats.lowStockCount }}</b> 件在售商品库存低于 10 件。
            在商品页点「库存」可以按正负增量调整，减少的会记一条库存流水。
          </p>
          <p v-else style="margin: 0; color: var(--owl-text-3)">暂无库存告警，所有在售商品库存充足。</p>
        </div>
      </section>

      <section class="adm-card">
        <div class="adm-card-head">
          <div>
            <h2>已发布公告</h2>
            <p>用户端首页顶部会展示最近的两条</p>
          </div>
          <RouterLink :to="{ name: 'admin-notices' }" class="adm-btn sm">去公告页</RouterLink>
        </div>
        <div class="adm-card-body">
          <div v-if="notices.length === 0" class="adm-empty" style="padding: 16px 0">
            还没有已发布的公告
          </div>
          <ul v-else style="margin: 0; padding: 0; list-style: none; display: grid; gap: 10px">
            <li
              v-for="item in notices"
              :key="item.id"
              style="display: flex; align-items: center; gap: 10px; font-size: 13px"
            >
              <span class="adm-tag info">{{ item.typeText }}</span>
              <span style="flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                {{ item.title }}
              </span>
              <button class="adm-btn link" type="button" @click="offline(item.id)">下线</button>
            </li>
          </ul>
        </div>
      </section>
    </div>

    <section class="adm-card">
      <div class="adm-card-head">
        <div>
          <h2>会员与账号</h2>
          <p>数据来自 c_user / s_admin 两张表</p>
        </div>
      </div>
      <div class="adm-card-body" style="display: flex; gap: 28px; font-size: 13px">
        <div>
          <div style="color: var(--owl-text-3); font-size: 12px">注册会员（不含访客）</div>
          <div style="font-size: 20px; font-weight: 700; margin-top: 4px">{{ stats.userCount }}</div>
        </div>
        <div>
          <div style="color: var(--owl-text-3); font-size: 12px">在售商品</div>
          <div style="font-size: 20px; font-weight: 700; margin-top: 4px">{{ stats.productCount }}</div>
        </div>
      </div>
    </section>

    <div v-if="message" class="adm-toast" :class="{ 'is-error': isError }">
      {{ message }}
    </div>
  </template>
</template>
