<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAdminSession } from '@/stores/adminSession'
import './admin.css'

/**
 * 管理端外壳：左侧固定导航 + 右侧内容。
 * 标题走路由 meta，视图自己不画页头，保证每个页面顶栏一致。
 */
const route = useRoute()
const router = useRouter()
const session = useAdminSession()

interface NavItem {
  name: string
  label: string
  hint: string
  icon: string
}

const NAV: NavItem[] = [
  {
    name: 'admin-dashboard',
    label: '概览',
    hint: '成交、订单、库存',
    icon: 'M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z',
  },
  {
    name: 'admin-products',
    label: '商品',
    hint: '上架、改价、库存',
    icon: 'M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3zM4 7.5l8 4.5 8-4.5M12 12v9',
  },
  {
    name: 'admin-orders',
    label: '订单',
    hint: '发货、查明细',
    icon: 'M6 3h9l4 4v14H6zM15 3v5h4M9 13h7M9 17h5',
  },
  {
    name: 'admin-notices',
    label: '公告',
    hint: '发布、下线、置顶',
    icon: 'M4 10v4h3l6 4V6l-6 4H4zM17 9.2a4 4 0 0 1 0 5.6',
  },
  {
    name: 'admin-members',
    label: '会员',
    hint: '等级、积分、禁用',
    icon: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4.5 20a7.5 7.5 0 0 1 15 0',
  },
  {
    name: 'admin-logs',
    label: '操作日志',
    hint: '谁改了哪件商品',
    icon: 'M5 4h14v16H5zM9 8h6M9 12h6M9 16h4',
  },
]

const activeName = computed(() => route.name)

const title = computed(() => {
  const current = NAV.find((item) => item.name === activeName.value)
  if (current) return { main: current.label, sub: current.hint }
  return { main: '管理后台', sub: 'OWL 商城' }
})

onMounted(() => {
  // 刷新后拿 /me 验一次 token，服务端吊销过就自动踢回登录页
  void session.bootstrap().then((ok) => {
    if (!ok) void router.replace({ name: 'admin-login' })
  })
})

function logout() {
  session.logout()
  void router.replace({ name: 'admin-login' })
}
</script>

<template>
  <div class="adm-shell">
    <aside class="adm-side">
      <div class="adm-brand">
        <div class="adm-brand-mark">OWL</div>
        <div class="adm-brand-text">
          <b>OWL 管理后台</b>
          <span>Merchant Console</span>
        </div>
      </div>

      <nav class="adm-nav">
        <div class="adm-nav-group">运营</div>
        <RouterLink
          v-for="item in NAV"
          :key="item.name"
          :to="{ name: item.name }"
          class="adm-nav-item"
          :class="{ 'is-active': activeName === item.name }"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round" stroke-linecap="round" aria-hidden="true">
            <path :d="item.icon" />
          </svg>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="adm-side-foot">
        <b>owl-server</b><br />
        Spring Boot · MySQL · JWT
      </div>
    </aside>

    <div class="adm-main">
      <header class="adm-top">
        <div>
          <h1>{{ title.main }}</h1>
          <p>{{ title.sub }}</p>
        </div>
        <div class="adm-top-right">
          <RouterLink to="/" class="adm-btn sm">回到商城</RouterLink>
          <div class="adm-who">
            <b>{{ session.admin?.nickname ?? '管理员' }}</b>
            <span>{{ session.roleLabel }}</span>
          </div>
          <button class="adm-btn sm" type="button" @click="logout">退出</button>
        </div>
      </header>

      <div class="adm-body">
        <RouterView />
      </div>
    </div>
  </div>
</template>
