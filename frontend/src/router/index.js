import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/', name: 'Home', component: () => import('@/pages/Home.vue') },
  { path: '/product/:id', name: 'ProductDetail', component: () => import('@/pages/ProductDetail.vue') },
  { path: '/login', name: 'Login', component: () => import('@/pages/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/pages/Register.vue') },
  { path: '/cart', name: 'Cart', component: () => import('@/pages/Cart.vue') },
  { path: '/checkout', name: 'Checkout', component: () => import('@/pages/Checkout.vue') },
  { path: '/user', name: 'UserCenter', component: () => import('@/pages/UserCenter.vue') },
  { path: '/user/profile', name: 'UserProfile', component: () => import('@/pages/UserProfile.vue') },
  { path: '/user/shop/apply', name: 'ShopApply', component: () => import('@/pages/ShopApply.vue') },
  { path: '/user/addresses', name: 'AddressPage', component: () => import('@/pages/AddressPage.vue') },
  { path: '/user/order/:orderNo', name: 'UserOrderDetail', component: () => import('@/pages/UserOrderDetail.vue') },
  { path: '/vendor', name: 'VendorDashboard', component: () => import('@/pages/vendor/Dashboard.vue') },
  { path: '/vendor/products', name: 'VendorProducts', component: () => import('@/pages/vendor/Products.vue') },
  { path: '/vendor/orders', name: 'VendorOrders', component: () => import('@/pages/vendor/Orders.vue') },
  { path: '/vendor/order/:orderNo', name: 'VendorOrderDetail', component: () => import('@/pages/vendor/OrderDetail.vue') },
  { path: '/vendor/notifications', name: 'VendorNotifications', component: () => import('@/pages/vendor/Notifications.vue') },
  { path: '/vendor/flash-sales', name: 'VendorFlashSales', component: () => import('@/pages/vendor/FlashSales.vue') },
  { path: '/coupons', name: 'CouponCenter', component: () => import('@/pages/CouponCenter.vue') },
  { path: '/shop/:shopId', name: 'ShopPage', component: () => import('@/pages/ShopPage.vue') },
  { path: '/pay', name: 'PaymentPage', component: () => import('@/pages/PaymentPage.vue') },
  { path: '/pay-result', name: 'PayResult', component: () => import('@/pages/PayResult.vue') },
]

const savedPositions = {}

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (savedPositions[to.name]) {
      return false
    }
    return { top: 0 }
  },
})
let lastSync = 0
router.beforeEach(async () => {
  const auth = useAuthStore()
  if (!auth.isLoggedIn) return
  // 只有普通用户在等待审批通过时才需要刷新后端角色，商家/管理员无需轮询
  if (auth.user?.role !== 0) return
  if (Date.now() - lastSync < 60_000) return
  lastSync = Date.now()
  auth.fetchMe().catch(() => {})
  auth.fetchShop().catch(() => {})
})

export { savedPositions }
export default router
