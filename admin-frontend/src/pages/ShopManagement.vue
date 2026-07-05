<template>
  <div class="space-y-4 animate-fade-in">
    <div class="bg-white rounded-xl border border-gray-100 p-5">
      <h3 class="font-semibold text-gray-800 mb-1">店铺管理</h3>
      <p class="text-sm text-gray-500">查看系统内所有店铺信息及营业状态</p>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 text-left">
          <tr>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">店铺ID</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">店铺名称</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">LOGO</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">营业状态</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">审批状态</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="shop in shopList" :key="shop.id" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-5 py-3 text-sm text-gray-600">{{ shop.id }}</td>
            <td class="px-5 py-3">
              <span class="font-medium text-gray-800">{{ shop.name }}</span>
            </td>
            <td class="px-5 py-3">
              <img v-if="shop.logo" :src="shop.logo" class="w-10 h-10 rounded-lg object-cover" />
              <span v-else class="text-gray-400 text-sm">-</span>
            </td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                :class="shop.status === 1 ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'">
                {{ shop.status === 1 ? '营业中' : '已关闭' }}
              </span>
            </td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                :class="approvalClass(shop.approved)">
                {{ approvalText(shop.approved) }}
              </span>
            </td>
            <td class="px-5 py-3">
              <router-link :to="`/shops/${shop.id}?name=${encodeURIComponent(shop.name)}`"
                class="inline-flex items-center text-xs px-3 py-1.5 rounded-md bg-indigo-50 text-indigo-600 hover:bg-indigo-100 transition-colors font-medium whitespace-nowrap">
                进店浏览
              </router-link>
            </td>
          </tr>
          <tr v-if="shopList.length === 0 && !loading">
            <td colspan="6" class="px-5 py-12 text-center text-gray-400">暂无店铺数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { shopAPI } from '@/api'

const shopList = ref([])
const loading = ref(false)

function approvalText(v) {
  if (v === 1) return '已通过'
  if (v === 2) return '已拒绝'
  return '待审批'
}

function approvalClass(v) {
  if (v === 1) return 'bg-green-50 text-green-700'
  if (v === 2) return 'bg-red-50 text-red-700'
  return 'bg-yellow-50 text-yellow-700'
}

onMounted(async () => {
  loading.value = true
  try {
    shopList.value = await shopAPI.list() || []
  } catch (e) {
    console.error('加载店铺列表失败:', e)
  } finally {
    loading.value = false
  }
})
</script>
