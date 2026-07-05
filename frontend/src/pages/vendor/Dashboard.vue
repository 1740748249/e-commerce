<template>
  <div v-if="auth.isVendor" class="px-4 md:px-0">
    <!-- 头部欢迎 -->
    <div class="flex items-center gap-3 mb-6">
      <router-link to="/" class="md:hidden text-gray-400 active:text-gray-600 text-xl">&larr;</router-link>
      <div>
        <h1 class="text-lg md:text-2xl font-bold text-gray-800">{{ auth.user.shopName }}</h1>
        <p class="text-sm text-gray-400 mt-0.5">欢迎回来，{{ auth.user.name }}</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 md:gap-5 mb-8">
      <!-- 商品总数 -->
      <router-link
        to="/vendor/products"
        class="bg-white rounded-2xl p-5 shadow-md hover:shadow-xl transition-all duration-300 group"
      >
        <div class="flex items-center justify-between">
          <div class="w-12 h-12 bg-gradient-to-br from-blue-100 to-blue-200 rounded-xl flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
            &#128230;
          </div>
          <div class="text-right">
            <div class="text-2xl md:text-3xl font-bold text-gray-800">{{ productStore.list.length }}</div>
            <div class="text-xs text-gray-400 mt-0.5">商品总数</div>
          </div>
        </div>
      </router-link>

      <!-- 订单总数 -->
      <router-link
        to="/vendor/orders"
        class="bg-white rounded-2xl p-5 shadow-md hover:shadow-xl transition-all duration-300 group"
      >
        <div class="flex items-center justify-between">
          <div class="w-12 h-12 bg-gradient-to-br from-green-100 to-green-200 rounded-xl flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
            &#128196;
          </div>
          <div class="text-right">
            <div class="text-2xl md:text-3xl font-bold text-gray-800">{{ order.shopOrders.length }}</div>
            <div class="text-xs text-gray-400 mt-0.5">订单总数</div>
          </div>
        </div>
      </router-link>

      <!-- 已支付(待发货) -->
      <router-link
        to="/vendor/orders"
        class="bg-gradient-to-br from-yellow-50 to-amber-50 rounded-2xl p-5 shadow-md hover:shadow-xl transition-all duration-300 group border border-yellow-100"
      >
        <div class="flex items-center justify-between">
          <div class="w-12 h-12 bg-gradient-to-br from-yellow-200 to-amber-200 rounded-xl flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
            &#9200;
          </div>
          <div class="text-right">
            <div class="text-2xl md:text-3xl font-bold text-yellow-600">{{ order.shopOrders.filter(o => o.status === '已支付').length }}</div>
            <div class="text-xs text-yellow-600 mt-0.5">已支付(待发货)</div>
          </div>
        </div>
      </router-link>

      <!-- 未读通知 -->
      <router-link
        to="/vendor/notifications"
        class="bg-white rounded-2xl p-5 shadow-md hover:shadow-xl transition-all duration-300 group relative overflow-hidden"
      >
        <div class="flex items-center justify-between">
          <div class="w-12 h-12 bg-gradient-to-br from-orange-100 to-orange-200 rounded-xl flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
            &#128276;
          </div>
          <div class="text-right">
            <div class="text-2xl md:text-3xl font-bold text-gray-800">{{ notif.unreadCount }}</div>
            <div class="text-xs text-gray-400 mt-0.5">未读通知</div>
          </div>
        </div>
        <span v-if="notif.unreadCount" class="absolute top-3 right-3 w-3 h-3 bg-red-500 rounded-full animate-pulse"></span>
      </router-link>
    </div>

    <!-- 快捷操作 -->
    <div class="bg-white rounded-2xl p-5 md:p-6 shadow-md mb-6">
      <h2 class="text-base md:text-lg font-semibold text-gray-800 mb-4">快捷操作</h2>
      <div class="flex flex-wrap gap-3">
        <router-link
          to="/vendor/products"
          class="flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl text-sm font-medium hover:shadow-lg hover:shadow-orange-200 transition-all duration-300 hover:-translate-y-0.5"
        >
          <span>&#128230;</span> 商品管理
        </router-link>
        <router-link
          to="/vendor/orders"
          class="flex items-center gap-2 px-5 py-2.5 border-2 border-gray-200 rounded-xl text-sm text-gray-600 font-medium hover:border-orange-300 hover:text-orange-600 transition-all duration-300"
        >
          <span>&#128196;</span> 订单管理
        </router-link>
        <router-link
          to="/vendor/flash-sales"
          class="flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-red-500 to-orange-500 text-white rounded-xl text-sm font-medium hover:shadow-lg hover:shadow-orange-200 transition-all duration-300 hover:-translate-y-0.5"
        >
          <span>&#9889;</span> 秒杀管理
        </router-link>
        <router-link
          to="/vendor/notifications"
          class="flex items-center gap-2 px-5 py-2.5 border-2 border-gray-200 rounded-xl text-sm text-gray-600 font-medium hover:border-orange-300 hover:text-orange-600 transition-all duration-300"
        >
          <span>&#128276;</span> 消息通知
        </router-link>
      </div>
    </div>

    <!-- 最近订单 -->
    <div class="bg-white rounded-2xl p-5 md:p-6 shadow-md">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-base md:text-lg font-semibold text-gray-800">最近订单</h2>
        <router-link to="/vendor/orders" class="text-sm text-orange-500 font-medium hover:text-orange-600">
          查看全部 &rarr;
        </router-link>
      </div>
      <div v-if="order.shopOrders.length === 0" class="text-center py-8 text-gray-400">
        暂无订单
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="o in order.shopOrders.slice(0, 3)"
          :key="o.id"
          class="flex items-center justify-between p-3 bg-gray-50 rounded-xl"
        >
          <div>
            <p class="text-sm font-medium text-gray-800">{{ o.productName }}</p>
            <p class="text-xs text-gray-400 mt-0.5">订单号: #{{ o.orderNo || o.id }}</p>
          </div>
          <div class="text-right">
            <span :class="statusClass(o.status)" class="text-xs px-2.5 py-1 rounded-full font-medium">
              {{ o.status }}
            </span>
            <p class="text-sm font-bold text-orange-500 mt-1">¥{{ ((o.total || 0) / 100).toFixed(0) }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 非商家账号提示 -->
  <div v-else class="text-center py-24">
    <div class="w-24 h-24 mx-auto mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center">
      <span class="text-4xl">&#127978;</span>
    </div>
    <p class="text-gray-500 font-medium text-lg">商家专属后台</p>
    <p class="text-sm text-gray-400 mt-1">请使用商家账号登录</p>
    <router-link
      to="/login"
      class="inline-block mt-6 px-10 py-3 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl font-medium hover:shadow-lg hover:shadow-orange-200 transition-all duration-300"
    >
      去登录
    </router-link>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useOrderStore } from '@/stores/order'
import { useProductStore } from '@/stores/product'
import { useNotificationStore } from '@/stores/notification'

const auth = useAuthStore()
const order = useOrderStore()
const productStore = useProductStore()
const notif = useNotificationStore()

const statusClass = (s) => {
  switch (s) {
    case '待支付': return 'bg-red-100 text-red-700'
    case '已支付': return 'bg-yellow-100 text-yellow-700'
    case '已发货': return 'bg-blue-100 text-blue-700'
    case '已完成': return 'bg-green-100 text-green-700'
    case '已取消': return 'bg-gray-100 text-gray-500'
    case '退款中': return 'bg-purple-100 text-purple-700'
    case '已退款': return 'bg-slate-100 text-slate-600'
    default: return 'bg-gray-100 text-gray-600'
  }
}

onMounted(async () => {
  await Promise.all([
    productStore.fetchMyProducts(),
    order.fetchShopOrders(),
    notif.fetchShop(),
  ])
})
</script>
