<template>
  <div class="px-4 md:px-0 py-4 md:py-6 max-w-3xl mx-auto">
    <div class="flex items-center gap-2 mb-4 md:mb-6">
      <span class="text-2xl">🎫</span>
      <h1 class="text-xl md:text-2xl font-bold text-gradient">优惠券中心</h1>
    </div>

    <div class="flex gap-1 bg-white rounded-xl p-1 shadow-sm mb-4 md:mb-6">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        @click="switchTab(tab.key)"
        :class="[
          'flex-1 py-2 md:py-2.5 rounded-lg text-xs md:text-sm font-medium transition-all duration-300',
          activeTab === tab.key ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-md' : 'text-gray-500'
        ]"
      >
        {{ tab.label }}
        <span v-if="tab.key === 'available' && store.available.length" class="ml-1">({{ store.available.length }})</span>
      </button>
    </div>

    <!-- 领券中心 -->
    <template v-if="activeTab === 'all'">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3 md:gap-4">
        <div v-for="c in store.allCoupons" :key="c.id" class="coupon-card relative bg-white rounded-2xl overflow-hidden shadow-md border transition-all duration-300 hover:shadow-lg hover:-translate-y-0.5" :class="c.claimed ? 'opacity-60' : ''">
          <div class="flex">
            <div class="flex-shrink-0 w-28 md:w-32 flex flex-col items-center justify-center py-4 md:py-5 bg-gradient-to-br text-white" :class="c.color || 'from-orange-500 to-amber-500'">
              <template v-if="c.type === 'no_threshold'">
                <span class="text-3xl md:text-4xl font-extrabold leading-none">{{ c.yuanReduce }}</span>
                <span class="text-xs md:text-sm mt-1 opacity-90">无门槛</span>
              </template>
              <template v-else>
                <span class="text-xs opacity-80">满{{ c.yuanThreshold }}</span>
                <span class="text-3xl md:text-4xl font-extrabold leading-none">减{{ c.yuanReduce }}</span>
              </template>
            </div>
            <div class="flex-1 flex flex-col justify-between px-3 md:px-4 py-3 md:py-4">
              <div>
                <h3 class="text-sm md:text-base font-bold text-gray-800">{{ c.name }}</h3>
                <p class="text-[10px] md:text-xs text-gray-400 mt-0.5">{{ c.description }}</p>
              </div>
              <div class="flex items-center justify-between mt-2">
                <span class="text-[10px] md:text-xs text-gray-400">有效期 {{ c.validDays }} 天</span>
                <button v-if="c.claimed" disabled class="text-[10px] md:text-xs px-3 py-1 rounded-lg bg-gray-100 text-gray-400 cursor-not-allowed">已领取</button>
                <button v-else @click.stop="handleClaim(c)" class="text-[10px] md:text-xs px-3 py-1 rounded-lg font-bold text-white bg-gradient-to-r from-orange-500 to-amber-500 hover:shadow-md hover:shadow-orange-200 transition-all duration-300 active:scale-95">立即领取</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-if="activeTab === 'available'">
      <div v-if="store.available.length === 0" class="text-center py-16">
        <div class="w-16 h-16 mx-auto mb-3 bg-orange-100 rounded-full flex items-center justify-center text-2xl">🎫</div>
        <p class="text-gray-500 font-medium">暂无可用优惠券</p>
        <p class="text-sm text-gray-400 mt-1">快去领取吧</p>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3 md:gap-4">
        <div v-for="item in store.available" :key="item.id" class="relative bg-white rounded-2xl overflow-hidden shadow-md border border-orange-200">
          <div class="flex">
            <div class="flex-shrink-0 w-28 md:w-32 flex flex-col items-center justify-center py-4 md:py-5 bg-gradient-to-br from-orange-500 to-amber-500 text-white">
              <template v-if="item.type === 1">
                <span class="text-3xl md:text-4xl font-extrabold leading-none">{{ item.yuanReduce }}</span>
                <span class="text-xs md:text-sm mt-1 opacity-90">无门槛</span>
              </template>
              <template v-else>
                <span class="text-xs opacity-80">满{{ item.yuanThreshold }}</span>
                <span class="text-3xl md:text-4xl font-extrabold leading-none">减{{ item.yuanReduce }}</span>
              </template>
            </div>
            <div class="flex-1 flex flex-col justify-between px-3 md:px-4 py-3 md:py-4">
              <div>
                <h3 class="text-sm md:text-base font-bold text-gray-800">{{ item.couponName || item.name }}</h3>
                <p class="text-[10px] md:text-xs text-gray-400 mt-0.5">{{ item.description || (item.type === 1 ? `无门槛减${item.yuanReduce}元` : `满${item.yuanThreshold}减${item.yuanReduce}`) }}</p>
              </div>
              <div class="flex items-center justify-between mt-2">
                <span class="text-[10px] text-orange-500 font-medium">{{ formatExpire(item.expireAt) }}到期</span>
                <span class="text-[10px] md:text-xs px-2 py-0.5 rounded-full bg-orange-100 text-orange-600 font-bold">去使用</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <template v-if="activeTab === 'used' || activeTab === 'expired'">
      <div v-if="list.length === 0" class="text-center py-16">
        <div class="w-16 h-16 mx-auto mb-3 bg-gray-100 rounded-full flex items-center justify-center text-2xl">{{ activeTab === 'used' ? '✅' : '⏰' }}</div>
        <p class="text-gray-500 font-medium">{{ activeTab === 'used' ? '暂无使用记录' : '暂无过期优惠券' }}</p>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-3 md:gap-4">
        <div v-for="item in list" :key="item.id" class="relative bg-white rounded-2xl overflow-hidden shadow-sm border border-gray-100 opacity-50">
          <div class="flex">
            <div class="flex-shrink-0 w-28 md:w-32 flex flex-col items-center justify-center py-4 md:py-5 bg-gradient-to-br from-gray-400 to-gray-500 text-white grayscale">
              <template v-if="item.type === 1">
                <span class="text-3xl md:text-4xl font-extrabold leading-none">{{ item.yuanReduce }}</span>
              </template>
              <template v-else>
                <span class="text-xs opacity-80">满{{ item.yuanThreshold }}</span>
                <span class="text-3xl md:text-4xl font-extrabold leading-none">减{{ item.yuanReduce }}</span>
              </template>
            </div>
            <div class="flex-1 flex flex-col justify-between px-3 md:px-4 py-3 md:py-4">
              <h3 class="text-sm md:text-base font-bold text-gray-400">{{ item.couponName || item.name }}</h3>
              <div class="mt-2">
                <span class="text-[10px] md:text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-400">{{ activeTab === 'used' ? '已使用' : '已过期' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useCouponStore } from '@/stores/coupon'

const store = useCouponStore()
const activeTab = ref('all')

const tabs = [
  { key: 'all', label: '领券中心' },
  { key: 'available', label: '我的优惠券' },
  { key: 'used', label: '已使用' },
  { key: 'expired', label: '已过期' },
]

const list = computed(() => {
  if (activeTab.value === 'used') return store.used
  if (activeTab.value === 'expired') return store.expired
  return []
})

async function switchTab(key) {
  activeTab.value = key
  if (key === 'all') {
    await store.fetchAll()
  } else {
    await store.fetchMy()
  }
}

async function handleClaim(coupon) {
  try {
    await store.claim(coupon.id)
    ElMessage.success('领取成功')
  } catch (e) {
    ElMessage.error(e.message || '领取失败')
  }
}

function formatExpire(iso) {
  if (!iso) return '?'
  const d = new Date(iso)
  const hours = Math.floor((d - Date.now()) / 3600000)
  if (hours < 24) return hours + '小时后'
  return Math.floor(hours / 24) + '天后'
}

onMounted(async () => {
  await store.fetchAll()
})
</script>
