<template>
  <div class="space-y-6">
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
      <div v-for="card in statCards" :key="card.label"
        class="stat-card bg-white rounded-xl p-5 border border-gray-100">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">{{ card.label }}</p>
            <p class="text-2xl font-bold text-gray-900 mt-1">{{ card.value }}</p>
          </div>
          <div class="w-10 h-10 rounded-lg flex items-center justify-center text-lg" :class="card.bg">
            {{ card.icon }}
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="bg-white rounded-xl border border-gray-100 p-5">
        <h3 class="font-semibold text-gray-800 mb-4">快捷操作</h3>
        <div class="grid grid-cols-2 gap-3">
          <router-link v-for="action in quickActions" :key="action.path" :to="action.path"
            class="flex flex-col items-center gap-2 p-4 rounded-xl border border-gray-100 hover:border-indigo-200 hover:bg-indigo-50/50 transition-all cursor-pointer text-center">
            <span class="text-2xl">{{ action.icon }}</span>
            <span class="text-sm text-gray-600">{{ action.label }}</span>
          </router-link>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-gray-100 p-5">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-semibold text-gray-800">热销商品 Top 5</h3>
          <router-link to="/ranking" class="text-sm text-indigo-600 hover:text-indigo-800">查看全部 →</router-link>
        </div>
        <div class="space-y-3">
          <div v-for="item in topRanking" :key="item.rank"
            class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 transition-colors">
            <span class="w-6 text-center font-bold" :class="item.rank <= 3 ? 'text-orange-500' : 'text-gray-400'">
              {{ item.rank }}
            </span>
            <div class="flex-1 min-w-0">
              <p class="text-sm text-gray-800 truncate">{{ item.productName }}</p>
              <p class="text-xs text-gray-400">{{ item.shopName }}</p>
            </div>
            <div class="text-right">
              <p class="text-sm font-semibold text-orange-600">¥{{ (item.minPrice / 100).toFixed(2) }}</p>
              <p class="text-xs text-gray-400">{{ item.sales.toLocaleString() }} 件</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminAPI, productAPI } from '@/api'

const stats = ref({ totalUsers: 0, totalMerchants: 0, pendingMerchants: 0, totalOrders: 0, totalSales: 0 })
const topRanking = ref([])

const statCards = computed(() => [
  { label: '总用户数', value: stats.value.totalUsers, icon: '👥', bg: 'bg-blue-50' },
  { label: '入驻商家', value: stats.value.totalMerchants, icon: '🏪', bg: 'bg-green-50' },
  { label: '待审批', value: stats.value.pendingMerchants, icon: '⏳', bg: 'bg-amber-50' },
  { label: '订单总数', value: stats.value.totalOrders, icon: '📦', bg: 'bg-purple-50' },
  { label: '总销售额', value: '¥' + ((stats.value.totalSales || 0) / 100).toLocaleString(), icon: '💰', bg: 'bg-indigo-50' },
])

const quickActions = [
  { path: '/merchants/pending', icon: '✅', label: '商家审批' },
  { path: '/users', icon: '👥', label: '用户管理' },
  { path: '/ranking', icon: '🏆', label: '销售排行' },
  { path: '/users', icon: '🚫', label: '禁用管理' },
]

onMounted(async () => {
  try {
    const [s, r] = await Promise.all([
      adminAPI.statistics(),
      productAPI.getRanking(5, { cache: false }),
    ])
    if (s) stats.value = s
    if (r) topRanking.value = r
  } catch (e) {
    console.error('Dashboard fetch error:', e)
  }
})
</script>
