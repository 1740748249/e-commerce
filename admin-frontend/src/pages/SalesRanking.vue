<template>
  <div class="space-y-4 animate-fade-in">
    <div class="bg-white rounded-xl border border-gray-100 p-5">
      <h3 class="font-semibold text-gray-800 mb-1">商品销量排行榜</h3>
      <p class="text-sm text-gray-500">按累计销售量降序排列，实时反映商品热度</p>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 text-left">
          <tr>
            <th class="px-5 py-3 text-sm font-medium text-gray-500 w-16">排名</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">商品</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">店铺</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">单价</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500 text-right">累计销量</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="item in rankingList" :key="item.rank" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-5 py-3">
              <div class="flex items-center gap-2">
                <span v-if="item.rank === 1" class="text-xl">🥇</span>
                <span v-else-if="item.rank === 2" class="text-xl">🥈</span>
                <span v-else-if="item.rank === 3" class="text-xl">🥉</span>
                <span v-else class="w-7 text-center font-semibold text-gray-400">{{ item.rank }}</span>
              </div>
            </td>
            <td class="px-5 py-3">
              <p class="font-medium text-gray-800">{{ item.productName }}</p>
            </td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ item.shopName }}</td>
            <td class="px-5 py-3 text-sm font-medium text-orange-600">¥{{ (item.minPrice / 100).toFixed(2) }}</td>
            <td class="px-5 py-3 text-right">
              <div class="inline-flex items-center gap-2">
                <div class="w-20 h-2 bg-gray-100 rounded-full overflow-hidden"
                  :title="item.sales + ' 件'">
                  <div class="h-full bg-indigo-500 rounded-full transition-all"
                    :style="{ width: (item.sales / maxSales * 100) + '%' }"></div>
                </div>
                <span class="text-sm font-semibold text-gray-700 w-16 text-right">{{ item.sales.toLocaleString() }}</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { productAPI } from '@/api'

const rankingList = ref([])
const maxSales = ref(1)

const CACHE_KEY = 'ranking_cache'
const CACHE_TTL = 60_000

onMounted(async () => {
  const cached = sessionStorage.getItem(CACHE_KEY)
  if (cached) {
    try {
      const { list, ts } = JSON.parse(cached)
      if (Date.now() - ts < CACHE_TTL) {
        rankingList.value = list
        maxSales.value = Math.max(...list.map(r => r.sales), 1)
      }
    } catch { /* ignore */ }
  }

  try {
    const data = await productAPI.getRanking(10, { cache: false })
    rankingList.value = data || []
    maxSales.value = Math.max(...rankingList.value.map(r => r.sales), 1)
    sessionStorage.setItem(CACHE_KEY, JSON.stringify({ list: data, ts: Date.now() }))
  } catch { /* ignore */ }
})
</script>
