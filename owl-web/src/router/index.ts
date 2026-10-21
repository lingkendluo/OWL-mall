import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由表与开发文档 1.4 清单一一对应，全部路由级懒加载（首屏只下载首页 chunk）。
 * 一期先落地登录页 + 首页占位，其余路由按 D1-D4 排期逐个补齐。
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/home/HomeView.vue'),
      meta: { title: 'OWL 商城' },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录 · OWL 商城', blank: true },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
