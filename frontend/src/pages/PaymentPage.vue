<template>
  <!-- 加载中的遮罩层 -->
  <div v-if="paying || error || success" class="fixed inset-0 flex items-center justify-center bg-white" style="z-index:10000">
    <div class="text-center max-w-md px-4">

      <!-- 正在跳转支付宝 -->
      <div v-if="paying" class="space-y-4">
        <div class="w-12 h-12 border-4 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
        <p class="text-gray-700 font-medium text-lg">正在跳转到支付页面...</p>
        <p class="text-gray-400 text-sm">请勿关闭窗口</p>
      </div>

      <!-- 支付完成 -->
      <div v-else-if="success" class="space-y-4">
        <div class="text-5xl mb-3">✅</div>
        <p class="text-green-500 font-medium text-lg">支付处理完成</p>
        <p class="text-gray-400 text-sm">正在返回...</p>
      </div>

      <!-- 错误 -->
      <div v-else-if="error" class="space-y-4">
        <div class="text-5xl mb-3">😞</div>
        <p class="text-red-500 font-medium text-lg">{{ error }}</p>
        <div class="flex gap-3 justify-center mt-6">
          <button @click="$router.back()" class="px-5 py-2.5 text-sm text-gray-600 border-2 border-gray-200 rounded-xl hover:bg-gray-50 transition-colors font-medium">返回</button>
          <button @click="retry" class="px-5 py-2.5 text-sm bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl hover:shadow-lg hover:shadow-orange-200 transition-all font-medium">重试</button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const paying = ref(true)
const success = ref(false)
const error = ref('')
let currentOrderNo = ''
let paymentIframe = null
let messageHandler = null

async function doPay(orderNo) {
  paying.value = true
  error.value = ''
  success.value = false

  const token = localStorage.getItem('token') || ''
  try {
    const res = await fetch(`http://localhost:8080/payment/pay?orderNo=${orderNo}`, {
      method: 'POST',
      headers: { 'Authorization': token ? `Bearer ${token}` : '' },
    })

    if (!res.ok) {
      const ct = res.headers.get('content-type') || ''
      if (ct.includes('application/json')) {
        const json = await res.json()
        throw new Error(json.message || `支付请求失败 (${res.status})`)
      }
      throw new Error(`支付服务异常 (HTTP ${res.status})`)
    }

    const ct = res.headers.get('content-type') || ''
    if (ct.includes('application/json')) {
      const json = await res.json()
      if (json.code && json.code !== 200) throw new Error(json.message || '支付请求失败')
      throw new Error('支付服务返回异常数据格式')
    }

    const html = await res.text()
    if (!html || html.trim().length === 0) {
      throw new Error('支付页面加载失败，请稍后重试')
    }

    // 创建全屏 iframe，在 iframe 中渲染支付宝支付表单
    paymentIframe = document.createElement('iframe')
    paymentIframe.style.cssText =
      'position:fixed;top:0;left:0;width:100%;height:100%;border:none;z-index:9999;background:#fff'
    document.body.appendChild(paymentIframe)

    const doc = paymentIframe.contentDocument || paymentIframe.contentWindow.document
    doc.open()
    doc.write(html)
    doc.close()

    // 1.5 秒后隐藏加载遮罩（支付宝表单会自动提交跳转）
    setTimeout(() => { paying.value = false }, 1500)

    // 监听 iframe 返回的 postMessage
    messageHandler = (event) => {
      if (event.data && event.data.type === 'ALIPAY_RETURN') {
        cleanup()
        success.value = true
        setTimeout(() => {
          router.replace(`/pay-result?orderNo=${event.data.orderNo}`)
        }, 600)
      }
    }
    window.addEventListener('message', messageHandler)

  } catch (e) {
    cleanup()
    error.value = e.message || '支付失败，请稍后重试'
    paying.value = false
  }
}

function cleanup() {
  if (messageHandler) {
    window.removeEventListener('message', messageHandler)
    messageHandler = null
  }
  if (paymentIframe) {
    document.body.removeChild(paymentIframe)
    paymentIframe = null
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

onUnmounted(() => {
  cleanup()
})
</script>
