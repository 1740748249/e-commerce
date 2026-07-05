<template>
  <div class="flex items-center justify-center min-h-[70vh] px-4">
    <div class="text-center max-w-md">
      <!-- Loading -->
      <div v-if="syncing" class="space-y-4">
        <div class="w-12 h-12 border-4 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
        <p class="text-gray-700 font-medium text-lg">正在同步支付结果...</p>
        <p class="text-gray-400 text-sm">请稍候</p>
      </div>

      <!-- Success -->
      <div v-else-if="success" class="space-y-4">
        <div class="text-5xl">✅</div>
        <p class="text-green-600 font-bold text-xl">支付成功</p>
        <p class="text-gray-400 text-sm">订单号：{{ orderNo }}</p>
        <div class="flex gap-3 justify-center mt-6">
          <router-link :to="`/user/order/${orderNo}`" class="px-5 py-2.5 text-sm bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl font-medium hover:shadow-lg transition-all">查看订单</router-link>
          <router-link to="/user" class="px-5 py-2.5 text-sm text-gray-600 border-2 border-gray-200 rounded-xl font-medium hover:bg-gray-50 transition-colors">返回个人中心</router-link>
        </div>
      </div>

      <!-- Error -->
      <div v-else class="space-y-4">
        <div class="text-5xl">😞</div>
        <p class="text-gray-700 font-medium text-lg">{{ error || '支付结果同步失败' }}</p>
        <p class="text-gray-400 text-sm">订单号：{{ orderNo }}</p>
        <div class="flex gap-3 justify-center mt-6">
          <button @click="sync" class="px-5 py-2.5 text-sm bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl font-medium hover:shadow-lg transition-all">重试</button>
          <router-link to="/user" class="px-5 py-2.5 text-sm text-gray-600 border-2 border-gray-200 rounded-xl font-medium hover:bg-gray-50 transition-colors">返回个人中心</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { paymentAPI } from '@/api'

const route = useRoute()
const orderNo = ref('')
const syncing = ref(true)
const success = ref(false)
const error = ref('')

async function sync() {
  syncing.value = true
  error.value = ''
  try {
    const res = await paymentAPI.status(orderNo.value)
    if (res.status === 1) {
      success.value = true
    } else if (res.status === 0) {
      error.value = '支付尚未完成，请确认是否已完成付款'
    } else {
      error.value = res.statusText || '支付状态异常'
    }
  } catch (e) {
    error.value = e.message || '同步失败，请稍后重试'
  }
  syncing.value = false
}

onMounted(() => {
  orderNo.value = route.query.orderNo || ''
  if (!orderNo.value) {
    error.value = '缺少订单号'
    syncing.value = false
    return
  }
  sync()
})
</script>
