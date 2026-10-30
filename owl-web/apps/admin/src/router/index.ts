import { createRouter, createWebHistory } from 'vue-router'
import { useAdminSession } from '@/stores/adminSession'

/**
 * 管理端路由表。
 *
 * 独立运行在 5174 后不再需要 /admin 前缀：
 *   /login      登录页
 *   /           概览（AdminLayout 下的默认子路由）
 *
 * 登录态只认 adminSession，和用户端的会员登录互不影响。
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'admin-login',
      component: () => import('@/views/admin/AdminLoginView.vue'),
      meta: { title: '管理后台登录 · OWL', blank: true },
    },
    {
      path: '/',
      component: () => import('@/views/admin/AdminLayout.vue'),
      children: [
        {
          path: '',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/AdminDashboardView.vue'),
          meta: { title: '概览 · OWL 管理后台' },
        },
        {
          path: 'products',
          name: 'admin-products',
          component: () => import('@/views/admin/AdminProductsView.vue'),
          meta: { title: '商品管理 · OWL 管理后台' },
        },
        {
          path: 'orders',
          name: 'admin-orders',
          component: () => import('@/views/admin/AdminOrdersView.vue'),
          meta: { title: '订单管理 · OWL 管理后台' },
        },
        {
          path: 'notices',
          name: 'admin-notices',
          component: () => import('@/views/admin/AdminNoticesView.vue'),
          meta: { title: '公告管理 · OWL 管理后台' },
        },
        {
          path: 'members',
          name: 'admin-members',
          component: () => import('@/views/admin/AdminMembersView.vue'),
          meta: { title: '会员管理 · OWL 管理后台' },
        },
        {
          path: 'logs',
          name: 'admin-logs',
          component: () => import('@/views/admin/AdminLogsView.vue'),
          meta: { title: '操作日志 · OWL 管理后台' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  // 守卫里才取 store：此时 pinia 已经 install 完成
  const admin = useAdminSession()
  if (to.name === 'admin-login') {
    return admin.isLoggedIn ? { name: 'admin-dashboard' } : true
  }
  return admin.isLoggedIn ? true : { name: 'admin-login' }
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : 'OWL 管理后台'
  document.title = title
})

export default router
