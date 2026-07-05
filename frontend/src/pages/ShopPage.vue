<template>
  <div class="px-4 md:px-0">
    <div v-if="loading" class="text-center py-24 text-gray-400">加载中...</div>

    <template v-else>
      <!-- 店铺信息卡片 -->
      <div class="bg-white rounded-2xl p-6 md:p-8 shadow-md mb-6">
        <div class="flex items-center gap-4">
          <div class="w-16 h-16 bg-gradient-to-br from-orange-100 to-orange-200 rounded-2xl flex items-center justify-center text-orange-600 font-bold text-2xl shrink-0">
            {{ shopName?.[0] || '店' }}
          </div>
          <div class="min-w-0">
            <h1 class="text-xl font-bold text-gray-800 truncate">{{ shopName }}</h1>
            <p class="text-sm text-gray-400 mt-1 flex items-center gap-1">
              <span class="w-2 h-2 bg-green-400 rounded-full"></span>营业中
            </p>
          </div>
        </div>
      </div>

      <!-- 排序栏 -->
      <div class="flex items-center gap-2 mb-4 bg-white rounded-xl px-4 py-2 shadow-sm overflow-x-auto">
        <span class="text-xs text-gray-400 shrink-0">排序：</span>
        <button
          v-for="opt in sortOptions"
          :key="opt.value"
          @click="changeSort(opt.value)"
          :class="[
            'px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors',
            currentSort === opt.value
              ? 'bg-orange-500 text-white shadow-sm'
              : 'text-gray-500 hover:bg-gray-100'
          ]"
        >{{ opt.label }}</button>
      </div>

      <!-- 商品列表 -->
      <div v-if="products.length === 0" class="text-center py-16 text-gray-400">该店铺暂无商品</div>
      <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-5">
        <div
          v-for="(item, index) in products"
          :key="item.id"
          class="product-card bg-white rounded-2xl overflow-hidden shadow-md cursor-pointer group animate-fade-in-up"
          :style="{ animationDelay: `${index * 50}ms` }"
        >
          <div class="relative overflow-hidden" @click="$router.push(`/product/${item.id}`)">
            <img :src="item.image" :alt="item.name" class="w-full aspect-square object-cover transition-transform duration-500 group-hover:scale-110" loading="lazy" />
            <div v-if="item.sales > 50" class="absolute top-2 left-2">
              <span class="bg-gradient-to-r from-red-500 to-orange-500 text-white text-[10px] px-2 py-0.5 rounded-full shadow-md">热卖</span>
            </div>
          </div>
          <div class="p-3 md:p-4" @click="$router.push(`/product/${item.id}`)">
            <h3 class="text-xs md:text-sm font-medium text-gray-800 truncate group-hover:text-orange-600 transition-colors">{{ item.name }}</h3>
            <div class="flex items-center justify-between mt-2">
              <div class="flex items-baseline gap-0.5">
                <span class="text-xs text-orange-500">¥</span>
                <span class="text-base md:text-xl font-bold text-orange-500">{{ (item.minPrice / 100).toFixed(0) }}</span>
              </div>
              <span class="text-[10px] md:text-xs text-gray-400">已售 {{ item.sales }}</span>
            </div>
          </div>
          <div class="px-3 pb-3 md:px-4 md:pb-4">
            <button
              @click.stop="handleBuyNow(item)"
              class="w-full py-2 bg-gradient-to-r from-orange-500 to-amber-500 text-white text-xs md:text-sm font-medium rounded-xl hover:shadow-lg hover:shadow-orange-200 active:scale-[0.98] transition-all">
              立即购买
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { shopPublicAPI } from '@/api'
import { useCheckoutStore } from '@/stores/checkout'

const route = useRoute()
const router = useRouter()
const checkout = useCheckoutStore()
const shopName = ref('')
const products = ref([])
const loading = ref(true)
const currentSort = ref('default')

const sortOptions = [
  { label: '默认', value: 'default' },
  { label: '价格 ↑', value: 'price_asc' },
  { label: '价格 ↓', value: 'price_desc' },
  { label: '销量', value: 'sales' },
]

async function fetchProducts() {
  loading.value = true
  const shopId = route.params.shopId
  try {
    const res = await shopPublicAPI.products(shopId, { page: 1, size: 50, sort: currentSort.value })
    const list = res.list || res.records || []
    products.value = list
    shopName.value = list[0]?.shopName || ''
  } catch {
    products.value = []
  }
  if (!shopName.value) {
    try {
      const shops = await shopPublicAPI.list()
      const shop = shops.find(s => String(s.id) === String(shopId))
      if (shop) shopName.value = shop.name
    } catch { /* ignore */ }
  }
  loading.value = false
}

function handleBuyNow(item) {
  checkout.setBuyNowItem({
    productId: item.id,
    skuId: 0,
    name: item.name,
    price: item.minPrice || item.price || 0,
    image: item.image || item.firstImage || '',
    shopId: item.shopId || 0,
    shopName: shopName.value || '',
    quantity: 1,
  })
  router.push('/checkout')
}

function changeSort(val) {
  if (currentSort.value === val) return
  currentSort.value = val
  fetchProducts()
}

onMounted(() => {
  fetchProducts()
})
</script>
