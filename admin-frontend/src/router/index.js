import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue'),
    meta: { guest: true },
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/pages/Dashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/merchants/pending',
    name: 'MerchantApproval',
    component: () => import('@/pages/MerchantApproval.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/users',
    name: 'UserManagement',
    component: () => import('@/pages/UserManagement.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/flash-sessions',
    name: 'FlashSessionManage',
    component: () => import('@/pages/FlashSessionManage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/flash-sales/approval',
    name: 'FlashSaleApproval',
    component: () => import('@/pages/FlashSaleApproval.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/coupons',
    name: 'CouponManage',
    component: () => import('@/pages/CouponManage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/ranking',
    name: 'SalesRanking',
    component: () => import('@/pages/SalesRanking.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/shops',
    name: 'ShopManagement',
    component: () => import('@/pages/ShopManagement.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/stores',
    name: 'StoresPublic',
    component: () => import('@/pages/StoresPublic.vue'),
  },
  {
    path: '/shops/:shopId',
    name: 'ShopProducts',
    component: () => import('@/pages/ShopProducts.vue'),
  },
  {
    path: '/products/:id',
    name: 'ProductDetail',
    component: () => import('@/pages/ProductDetail.vue'),
  },
  {
    path: '/orders',
    name: 'OrderManagement',
    component: () => import('@/pages/OrderManagement.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/orders/:orderNo',
    name: 'OrderDetail',
    component: () => import('@/pages/OrderDetail.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/notifications',
    name: 'NotificationManage',
    component: () => import('@/pages/NotificationManage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/pay',
    name: 'PaymentPage',
    component: () => import('@/pages/PaymentPage.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    next('/login')
  } else if (to.meta.guest && auth.isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router
