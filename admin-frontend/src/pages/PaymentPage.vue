<template>
  <div class="flex items-center justify-center min-h-[60vh]">
    <div class="text-center max-w-md">
      <!-- Loading -->
      <div v-if="paying" class="space-y-4">
        <div class="w-10 h-10 border-3 border-indigo-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
        <p class="text-gray-600 font-medium">正在跳转到支付页面...</p>
        <p class="text-gray-400 text-sm">请勿关闭窗口</p>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="space-y-4">
        <div class="text-4xl mb-2">😞</div>
        <p class="text-red-500 font-medium">{{ error }}</p>
        <div class="flex gap-3 justify-center mt-4">
          <button @click="$router.back()" class="px-4 py-2 text-sm text-gray-600 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">返回</button>
          <button @click="retry" class="px-4 py-2 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors">重试</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const paying = ref(true)
const error = ref('')
let currentOrderNo = ''

async function doPay(orderNo) {
  paying.value = true
  error.value = ''

  const token = localStorage.getItem('admin_token') || ''
  try {
    const res = await fetch(`/api/payment/pay?orderNo=${orderNo}`, {
      method: 'POST',
      headers: {
        'Authorization': token ? `Bearer ${token}` : '',
      },
    })

    if (!res.ok) {
      const contentType = res.headers.get('content-type') || ''
      if (contentType.includes('application/json')) {
        const json = await res.json()
        throw new Error(json.message || `支付请求失败 (${res.status})`)
      }
      throw new Error(`支付服务异常 (HTTP ${res.status})`)
    }

    const contentType = res.headers.get('content-type') || ''
    if (contentType.includes('application/json')) {
      const json = await res.json()
      if (json.code && json.code !== 200) {
        throw new Error(json.message || '支付请求失败')
      }
      throw new Error('支付服务返回异常数据格式')
    }

    const html = await res.text()
    if (!html || html.trim().length === 0) {
      throw new Error('支付页面加载失败，请稍后重试')
    }

    document.open()
    document.write(html)
    document.close()
  } catch (e) {
    error.value = e.message || '支付失败，请稍后重试'
    paying.value = false
  }
}

function retry() {
  if (currentOrderNo) doPay(currentOrderNo)
}

onMounted(() => {
  const orderNo = route.query.orderNo
  if (!orderNo) {
    error.value = '缺少订单号，请从订单页面重新发起支付'
    paying.value = false
    return
  }
  currentOrderNo = orderNo
  doPay(orderNo)
})
</script>
