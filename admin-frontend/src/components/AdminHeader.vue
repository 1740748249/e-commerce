<template>
  <header class="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 sticky top-0 z-20">
    <div>
      <h2 class="text-lg font-semibold text-gray-800">{{ pageTitle }}</h2>
    </div>
    <div class="flex items-center gap-4">
      <span class="text-sm text-gray-500">
        当前管理员：<strong class="text-gray-800">{{ authStore.adminUser?.name }}</strong>
      </span>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const titles = {
  Dashboard: '仪表盘',
  MerchantApproval: '商家审批',
  UserManagement: '用户管理',
  SalesRanking: '销售排行',
  ShopManagement: '店铺管理',
  ShopProducts: '店铺浏览',
  ProductDetail: '商品详情',
}

const pageTitle = computed(() => titles[route.name] || '管理后台')
</script>
