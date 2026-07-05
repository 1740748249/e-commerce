<template>
  <div class="max-w-6xl mx-auto space-y-6 animate-fade-in">
    <!-- 店铺信息 -->
    <div class="bg-white rounded-xl border border-gray-100 p-6">
      <div class="flex items-center gap-4">
        <div class="w-14 h-14 bg-indigo-100 rounded-xl flex items-center justify-center text-2xl">
          🏪
        </div>
        <div>
          <h2 class="text-xl font-bold text-gray-800">{{ shopName || '店铺 #' + shopId }}</h2>
          <p class="text-sm text-gray-500 mt-0.5">全部商品</p>
        </div>
        <button @click="goBack" class="ml-auto text-sm text-gray-400 hover:text-gray-600 transition-colors">
          ← 返回
        </button>
      </div>
    </div>

    <!-- 排序栏 -->
    <div class="flex items-center gap-1 bg-white rounded-lg border border-gray-100 p-1 w-fit">
      <button v-for="s in sortOptions" :key="s.value" @click="currentSort = s.value"
        class="px-4 py-1.5 rounded-md text-sm font-medium transition-colors"
        :class="currentSort === s.value
          ? 'bg-indigo-500 text-white shadow-sm'
          : 'text-gray-500 hover:text-gray-700'">
        {{ s.label }}
      </button>
    </div>

    <!-- 商品列表 -->
    <div v-if="loading" class="text-center py-20 text-gray-400">加载中...</div>

    <div v-else-if="products.length === 0" class="bg-white rounded-xl border border-gray-100 py-20 text-center text-gray-400">
      暂无商品
    </div>

    <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <div v-for="product in products" :key="product.id"
        @click="router.push(`/products/${product.id}`)"
        class="bg-white rounded-xl border border-gray-100 overflow-hidden hover:shadow-lg hover:-translate-y-1 transition-all duration-200 group block cursor-pointer">
        <!-- 商品图片 -->
        <div class="aspect-square bg-gray-100 relative overflow-hidden">
          <img v-if="product.image" :src="product.image" :alt="product.name"
            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
          <div v-else class="w-full h-full flex items-center justify-center text-4xl text-gray-300">📦</div>
        </div>
        <!-- 商品信息 -->
        <div class="p-3 space-y-2">
          <h3 class="text-sm font-medium text-gray-800 line-clamp-2 leading-snug">{{ product.name }}</h3>
          <div class="flex items-baseline gap-1">
            <span class="text-base font-bold text-red-500">¥{{ (product.minPrice / 100).toFixed(2) }}</span>
            <span v-if="product.minPrice > 0" class="text-xs text-gray-400">起</span>
          </div>
          <div class="flex items-center justify-between text-xs text-gray-400">
            <span>{{ product.categoryName || '' }}</span>
            <span>已售 {{ product.sales || 0 }}</span>
          </div>
          <button
            @click.stop="handleBuyNow(product)"
            class="w-full mt-1 px-3 py-1.5 bg-indigo-500 text-white text-xs font-medium rounded-lg hover:bg-indigo-600 transition-colors">
            立即购买
          </button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-1">
      <button @click="goPage(currentPage - 1)" :disabled="currentPage <= 1"
        class="px-3 py-1.5 rounded-md text-sm border border-gray-200 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50 transition-colors">
        上一页
      </button>
      <button v-for="p in visiblePages" :key="p" @click="goPage(p)"
        class="w-9 h-9 rounded-md text-sm font-medium transition-colors"
        :class="p === currentPage
          ? 'bg-indigo-500 text-white'
          : 'border border-gray-200 text-gray-600 hover:bg-gray-50'">
        {{ p }}
      </button>
      <button @click="goPage(currentPage + 1)" :disabled="currentPage >= totalPages"
        class="px-3 py-1.5 rounded-md text-sm border border-gray-200 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50 transition-colors">
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { shopAPI, orderAPI } from '@/api'

const route = useRoute()
const router = useRouter()
const shopId = computed(() => Number(route.params.shopId))

const shopName = ref(route.query.name || '')
const products = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const totalPages = ref(0)
const currentSort = ref('default')

const sortOptions = [
  { label: '综合', value: 'default' },
  { label: '价格 ↑', value: 'price_asc' },
  { label: '价格 ↓', value: 'price_desc' },
  { label: '销量', value: 'sales' },
]

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const cur = currentPage.value
  let start = Math.max(1, cur - 2)
  let end = Math.min(total, cur + 2)
  if (end - start < 4) {
    if (start === 1) end = Math.min(total, start + 4)
    else start = Math.max(1, end - 4)
  }
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

async function fetchProducts() {
  loading.value = true
  try {
    const data = await shopAPI.getProducts(shopId.value, {
      page: currentPage.value,
      size: pageSize.value,
      sort: currentSort.value,
    })
    products.value = data.list || []
    totalPages.value = data.totalPages || 0
  } catch (e) {
    console.error('加载商品失败:', e)
    products.value = []
  } finally {
    loading.value = false
  }
}

async function handleBuyNow(product) {
  try {
    const orderNo = await orderAPI.buyNow({
      productId: product.id,
      skuId: 0,
      quantity: 1,
      addressId: null,
      couponId: null,
      remark: '',
    })
    ElMessage.success('下单成功，正在跳转支付...')
    router.push(`/pay?orderNo=${orderNo}`)
  } catch (e) {
    ElMessage.error(e.message || '下单失败')
  }
}

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  currentPage.value = p
}

function goBack() {
  router.back()
}

watch(currentSort, () => {
  currentPage.value = 1
  fetchProducts()
})

watch(currentPage, () => {
  fetchProducts()
})

// 初始化
fetchProducts()
</script>
