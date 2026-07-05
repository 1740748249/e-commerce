<template>
  <aside class="fixed left-0 top-0 h-screen w-60 bg-slate-900 text-white flex flex-col z-30">
    <div class="px-5 py-5 border-b border-slate-700">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 bg-indigo-500 rounded-lg flex items-center justify-center text-sm font-bold">多</div>
        <div>
          <div class="font-semibold text-sm">多多商城</div>
          <div class="text-xs text-slate-400">管理后台</div>
        </div>
      </div>
    </div>

    <nav class="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors block"
        :class="$route.path === item.path || (item.path !== '/' && $route.path.startsWith(item.path))
          ? 'bg-indigo-600 text-white'
          : 'text-slate-300 hover:bg-slate-800'"
      >
        <span class="text-lg w-5 text-center">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
        <span v-if="item.badge" class="ml-auto bg-red-500 text-white text-xs px-1.5 py-0.5 rounded-full">{{ item.badge }}</span>
      </router-link>
    </nav>

    <div class="px-3 py-4 border-t border-slate-700">
      <button @click="authStore.logout()"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-slate-400 hover:bg-slate-800 w-full transition-colors">
        <span class="text-lg">🚪</span>
        <span>退出登录</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const navItems = [
  { path: '/', label: '仪表盘', icon: '📊' },
  { path: '/merchants/pending', label: '商家审批', icon: '✅' },
  { path: '/shops', label: '店铺管理', icon: '🏪' },
  { path: '/flash-sessions', label: '秒杀场次', icon: '⚡' },
  { path: '/flash-sales/approval', label: '秒杀审核', icon: '🔍' },
  { path: '/users', label: '用户管理', icon: '👥' },
  { path: '/coupons', label: '优惠券管理', icon: '🎫' },
  { path: '/notifications', label: '通知管理', icon: '🔔' },
  { path: '/orders', label: '订单管理', icon: '📦' },
  { path: '/ranking', label: '销售排行', icon: '🏆' },
]
</script>
