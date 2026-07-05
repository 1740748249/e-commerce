<template>
  <div v-for="s in sessions" :key="s.sessionId" class="mb-6 md:mb-8">
    <!-- 秒杀头部 -->
    <div class="flex items-center justify-between mb-3 md:mb-4">
      <div class="flex items-center gap-1.5 md:gap-2">
        <span class="text-xl md:text-2xl">⚡</span>
        <h2 class="text-lg md:text-xl font-bold text-gradient">{{ s.sessionName || '限时秒杀' }}</h2>
      </div>
      <span
        v-if="status(s) === 'active'"
        class="text-xs md:text-sm text-red-500 font-medium flex items-center gap-1 shrink-0"
      >
        <span class="w-1.5 h-1.5 bg-red-500 rounded-full animate-pulse"></span>
        抢购中
      </span>
      <span v-else class="text-xs md:text-sm text-orange-500 font-medium shrink-0">即将开始</span>
    </div>

    <!-- 倒计时 -->
    <div class="flex items-center justify-between gap-2 mb-3 md:mb-4 flex-wrap">
      <span class="text-xs md:text-sm text-gray-500">
        {{ status(s) === 'upcoming' ? '距开始还剩' : '距结束还剩' }}
      </span>
      <div class="flex items-center gap-1 md:gap-1.5">
        <span class="bg-red-50 text-red-600 font-bold px-1.5 py-0.5 rounded text-xs md:text-sm">{{ countdown(s).hours }}</span>
        <span class="text-xs md:text-sm text-gray-500">时</span>
        <span class="bg-red-50 text-red-600 font-bold px-1.5 py-0.5 rounded text-xs md:text-sm">{{ countdown(s).minutes }}</span>
        <span class="text-xs md:text-sm text-gray-500">分</span>
        <span class="bg-red-50 text-red-600 font-bold px-1.5 py-0.5 rounded text-xs md:text-sm">{{ countdown(s).seconds }}</span>
        <span class="text-xs md:text-sm text-gray-500">秒</span>
      </div>
    </div>

    <!-- 秒杀商品列表 - 横向滚动 -->
    <div class="relative">
      <div class="flex gap-3 md:gap-4 overflow-x-auto pb-2 scrollbar-hide scroll-smooth">
        <div
          v-for="item in s.items"
          :key="item.id"
          class="flash-card shrink-0 w-40 md:w-48 bg-white rounded-2xl overflow-hidden shadow-md cursor-pointer group border border-red-100 hover:border-red-200 hover:shadow-lg hover:shadow-red-100/50 transition-all duration-300"
        >
          <div class="relative overflow-hidden" @click="$router.push(`/product/${item.productId}`)">
            <img :src="item.image" :alt="item.name"
              class="w-full aspect-square object-cover transition-transform duration-500 group-hover:scale-110" loading="lazy" />
            <div class="absolute top-0 left-0 bg-gradient-to-r from-red-500 to-orange-500 text-white text-[10px] px-2 py-0.5 rounded-br-lg font-bold">秒杀</div>
            <div class="absolute top-0 right-0 bg-red-500 text-white text-[10px] px-1.5 py-0.5 rounded-bl-lg font-bold">{{ discount(item) }}折</div>
          </div>

          <div class="p-2.5 md:p-3">
            <h4 class="text-xs md:text-sm font-medium text-gray-800 truncate group-hover:text-red-500 transition-colors"
              @click="$router.push(`/product/${item.productId}`)">{{ item.name }}</h4>

            <div class="flex items-baseline gap-1.5 mt-1">
              <div class="flex items-baseline gap-0.5">
                <span class="text-[10px] text-red-500">¥</span>
                <span class="text-sm md:text-lg font-bold text-red-500">{{ (item.flashPrice / 100).toFixed(0) }}</span>
              </div>
              <span class="text-[10px] md:text-xs text-gray-400 line-through">¥{{ (item.originalPrice / 100).toFixed(0) }}</span>
            </div>
            <p class="text-[10px] text-gray-400 mt-1">每人限购 {{ item.perUserLimit || 1 }} 件</p>

            <div class="mt-2">
              <div class="flex items-center justify-between text-[10px] text-gray-400 mb-0.5">
                <span>已抢{{ item.progress || percent(item) }}%</span>
                <span v-if="item.stock - item.sold < 10" class="text-red-500 font-medium">即将售罄</span>
              </div>
              <div class="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                <div class="h-full bg-gradient-to-r from-red-500 to-orange-500 rounded-full transition-all duration-500"
                  :style="{ width: (item.progress || percent(item)) + '%' }"></div>
              </div>
            </div>

            <button
              @click="flashBuy(item)"
              :disabled="item.sold >= item.stock || status(s) !== 'active'"
              class="w-full mt-2.5 py-1.5 md:py-2 rounded-xl text-xs font-bold text-white bg-gradient-to-r from-red-500 to-orange-500 hover:from-red-600 hover:to-orange-600 hover:shadow-md hover:shadow-red-200 transition-all duration-300 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ status(s) !== 'active' ? '未开始' : item.sold >= item.stock ? '已售罄' : '立即抢购' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useProductStore } from '@/stores/product'
import { useAuthStore } from '@/stores/auth'
import { useAddressStore } from '@/stores/address'
import { flashSaleAPI } from '@/api'

const router = useRouter()
const productStore = useProductStore()
const auth = useAuthStore()
const addressStore = useAddressStore()

const sessions = computed(() => (productStore.flashSales || []).filter(s => s.items && s.items.length))

const now = ref(Date.now())
let timer = null

function status(s) {
  if (!s) return 'ended'
  const start = new Date(s.sessionStartTime).getTime()
  const end = new Date(s.sessionEndTime).getTime()
  if (now.value < start) return 'upcoming'
  if (now.value < end) return 'active'
  return 'ended'
}

function countdown(s) {
  if (!s) return { hours: '00', minutes: '00', seconds: '00' }
  const target = status(s) === 'upcoming' ? new Date(s.sessionStartTime).getTime() : new Date(s.sessionEndTime).getTime()
  const diff = Math.max(0, target - now.value)
  return {
    hours: String(Math.floor(diff / 3600000)).padStart(2, '0'),
    minutes: String(Math.floor((diff % 3600000) / 60000)).padStart(2, '0'),
    seconds: String(Math.floor((diff % 60000) / 1000)).padStart(2, '0'),
  }
}

function discount(item) {
  if (!item.originalPrice || !item.flashPrice) return 0
  return Math.round((item.flashPrice / item.originalPrice) * 10)
}

function percent(item) {
  if (!item.stock) return 0
  return Math.round((item.sold / item.stock) * 100)
}

async function flashBuy(item) {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }
  const addr = addressStore.defaultAddress
  if (!addr) {
    ElMessage.warning('请先添加收货地址')
    return router.push('/address')
  }
  try {
    const res = await flashSaleAPI.order(item.id, { quantity: 1, addressId: addr.id })
    if (res?.orderNo) {
      router.push(`/pay?orderNo=${res.orderNo}`)
    } else {
      ElMessage.success('秒杀下单成功')
      productStore.fetchFlashSales()
    }
  } catch (e) {
    ElMessage.error(e.message || '秒杀失败')
  }
}

onMounted(() => {
  productStore.fetchFlashSales()
  addressStore.fetchList()
  timer = setInterval(() => { now.value = Date.now() }, 1000)
})
onUnmounted(() => { clearInterval(timer) })
</script>

<style scoped>
.scrollbar-hide::-webkit-scrollbar { display: none; }
.scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
</style>
