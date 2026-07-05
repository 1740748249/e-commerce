<template>
  <div class="max-w-6xl mx-auto py-8 px-4 space-y-6 animate-fade-in">
    <div class="text-center mb-8">
      <h1 class="text-2xl font-bold text-gray-800">多多商城</h1>
      <p class="text-gray-500 mt-1">选择店铺，开始购物</p>
    </div>

    <div v-if="loading" class="text-center py-20 text-gray-400">加载中...</div>

    <div v-else-if="shops.length === 0" class="text-center py-20 text-gray-400">
      暂无营业中的店铺
    </div>

    <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <router-link v-for="shop in shops" :key="shop.id"
        :to="`/shops/${shop.id}?name=${encodeURIComponent(shop.name)}`"
        class="bg-white rounded-xl border border-gray-100 p-5 hover:shadow-lg hover:-translate-y-1 transition-all duration-200 block">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 bg-indigo-100 rounded-xl flex items-center justify-center text-xl flex-shrink-0">
            🏪
          </div>
          <div class="min-w-0">
            <h3 class="font-semibold text-gray-800 truncate">{{ shop.name }}</h3>
            <p class="text-xs text-gray-400 mt-0.5">点击进店浏览</p>
          </div>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { shopAPI } from '@/api'

const shops = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const all = await shopAPI.list() || []
    shops.value = all.filter(s => s.status === 1 && s.approved === 1)
  } catch (e) {
    console.error('加载店铺列表失败:', e)
  } finally {
    loading.value = false
  }
})
</script>
