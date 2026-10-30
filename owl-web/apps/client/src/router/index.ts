import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 路由表与开发文档 1.4 清单一一对应，全部路由级懒加载（首屏只下载首页 chunk）。
 * requiresAuth 的页面由全局守卫拦截，未登录一律先回登录页，并把原地址记在 ?redirect 上。
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/home/HomeView.vue'),
      meta: { title: 'OWL 商城', requiresAuth: true },
    },
    {
      path: '/c/:catId',
      name: 'category',
      component: () => import('@/views/category/CategoryView.vue'),
      meta: { title: '分类 · OWL 商城', requiresAuth: true },
    },
    {
      path: '/item/:id',
      name: 'item',
      component: () => import('@/views/item/ProductDetailView.vue'),
      meta: { title: '商品详情 · OWL 商城', requiresAuth: true },
    },
    {
      path: '/cart',
      name: 'cart',
      component: () => import('@/views/cart/CartView.vue'),
      meta: { title: '购物车 · OWL 商城', requiresAuth: true },
    },
    {
      path: '/addresses',
      name: 'addresses',
      component: () => import('@/views/address/AddressView.vue'),
      meta: { title: '地址管理 · OWL 商城', requiresAuth: true },
    },
    {
      path: '/checkout',
      name: 'checkout',
      component: () => import('@/views/checkout/CheckoutView.vue'),
      meta: { title: '待付款 · OWL 商城', requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/profile/ProfileView.vue'),
      meta: { title: '个人中心 · OWL 商城', requiresAuth: true },
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('@/views/orders/OrderListView.vue'),
      meta: { title: '我的订单 · OWL 商城', requiresAuth: true },
    },
    {
      path: '/messages',
      name: 'messages',
      component: () => import('@/views/messages/MessagesView.vue'),
      meta: { title: '消息 · OWL 商城', requiresAuth: true },
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

router.beforeEach((to) => {
  // 守卫里才取 store：此时 pinia 已经 install 完成
  const auth = useAuthStore()

  if (!auth.isLoggedIn && to.meta.requiresAuth) {
    // 首页不用记，跳回去就是首页；其他页记下来，登录后原路返回
    const redirect = to.fullPath === '/' ? {} : { redirect: to.fullPath }
    return { name: 'login', query: redirect }
  }

  if (auth.isLoggedIn && to.name === 'login') {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : 'OWL 商城'
  document.title = title
})

export default router
