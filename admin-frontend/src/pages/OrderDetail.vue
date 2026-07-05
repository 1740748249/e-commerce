<template>
  <div class="space-y-4 animate-fade-in">
    <div class="flex items-center gap-3">
      <button @click="$router.back()" class="text-sm text-gray-500 hover:text-gray-700">&larr; 返回</button>
      <h3 class="font-semibold text-gray-800">订单详情</h3>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-400">加载中...</div>

    <template v-if="order && !loading">
      <!-- 待支付横幅 -->
      <div v-if="order.status === 0" class="bg-indigo-50 border border-indigo-200 rounded-xl p-4 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div class="flex items-center gap-2">
          <span class="text-lg">⏳</span>
          <span class="font-medium text-indigo-800">订单待支付</span>
          <span class="text-sm text-indigo-600">请尽快完成支付</span>
        </div>
        <div class="flex gap-2 w-full sm:w-auto">
          <button @click="handlePay"
            class="flex-1 sm:flex-none px-6 py-2.5 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition-colors text-sm">去支付</button>
          <button @click="handleCancel"
            class="flex-1 sm:flex-none px-4 py-2.5 border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50 transition-colors text-sm">取消订单</button>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-gray-100 p-5">
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-4">
          <div>
            <p class="text-sm text-gray-500">订单号 <span class="font-mono text-gray-800 ml-2">{{ order.orderNo }}</span></p>
            <p class="text-sm text-gray-500 mt-1">
              状态
              <span class="ml-2 text-xs px-2 py-0.5 rounded-full font-medium" :class="statusClass(order.status)">
                {{ order.statusText || statusLabel(order.status) }}
              </span>
            </p>
          </div>
          <div v-if="order.status !== 0" class="flex gap-2">
            <button v-if="order.status === 1" @click="handleShip"
              class="px-4 py-2 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 transition-colors">确认发货</button>
            <button v-if="order.status === 2" @click="handleComplete"
              class="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors">标记完成</button>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-4 text-sm">
          <div><span class="text-gray-500">收货人：</span><span class="text-gray-800">{{ order.receiverFullName || order.receiverName }}</span></div>
          <div><span class="text-gray-500">电话：</span><span class="text-gray-800">{{ order.receiverFullPhone || order.receiverPhone }}</span></div>
          <div class="col-span-2"><span class="text-gray-500">地址：</span><span class="text-gray-800">{{ order.receiverFullAddr || order.receiverAddr }}</span></div>
          <div v-if="order.remark"><span class="text-gray-500">备注：</span><span class="text-gray-800">{{ order.remark }}</span></div>
          <div v-if="order.payNo"><span class="text-gray-500">支付流水号：</span><span class="text-gray-800 font-mono">{{ order.payNo }}</span></div>
          <div v-if="order.payTime"><span class="text-gray-500">支付时间：</span><span class="text-gray-800">{{ order.payTime }}</span></div>
          <div v-if="order.cancelTime"><span class="text-gray-500">取消时间：</span><span class="text-gray-800">{{ order.cancelTime }}</span></div>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
        <table class="w-full">
          <thead class="bg-gray-50 text-left">
            <tr>
              <th class="px-5 py-3 text-sm font-medium text-gray-500">商品</th>
              <th class="px-5 py-3 text-sm font-medium text-gray-500">规格</th>
              <th class="px-5 py-3 text-sm font-medium text-gray-500">单价</th>
              <th class="px-5 py-3 text-sm font-medium text-gray-500">数量</th>
              <th class="px-5 py-3 text-sm font-medium text-gray-500">小计</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="item in order.items" :key="item.id">
              <td class="px-5 py-3">
                <div class="flex items-center gap-3">
                  <img v-if="item.productImage" :src="item.productImage" class="w-10 h-10 rounded-lg object-cover bg-gray-100" />
                  <span class="text-sm text-gray-800">{{ item.productName }}</span>
                </div>
              </td>
              <td class="px-5 py-3 text-sm text-gray-500">{{ item.skuName || '-' }}</td>
              <td class="px-5 py-3 text-sm text-gray-800">¥{{ (item.price / 100).toFixed(2) }}</td>
              <td class="px-5 py-3 text-sm text-gray-800">{{ item.quantity }}</td>
              <td class="px-5 py-3 text-sm font-medium text-gray-800">¥{{ ((item.price * item.quantity) / 100).toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="bg-white rounded-xl border border-gray-100 p-5 flex justify-end">
        <div class="text-right space-y-1">
          <p class="text-sm text-gray-500">商品金额：<span class="text-gray-800">¥{{ ((order.totalAmount) / 100).toFixed(2) }}</span></p>
          <p v-if="order.discountAmount" class="text-sm text-gray-500">优惠：<span class="text-red-500">-¥{{ (order.discountAmount / 100).toFixed(2) }}</span></p>
          <p class="text-lg font-semibold text-gray-800">实付：¥{{ (order.payAmount != null ? order.payAmount : (order.totalAmount - order.discountAmount)) / 100 }}</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderAPI } from '@/api'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(true)

const statusOptions = [
  { value: 0, label: '待支付' },
  { value: 1, label: '已支付' },
  { value: 2, label: '已发货' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已取消' },
  { value: 5, label: '已退款' },
]

function statusLabel(status) {
  const m = statusOptions.find(s => s.value === status)
  return m ? m.label : '未知'
}

function statusClass(status) {
  const map = {
    0: 'bg-yellow-50 text-yellow-700',
    1: 'bg-blue-50 text-blue-700',
    2: 'bg-purple-50 text-purple-700',
    3: 'bg-green-50 text-green-700',
    4: 'bg-gray-100 text-gray-500',
    5: 'bg-red-50 text-red-700',
  }
  return map[status] || 'bg-gray-100 text-gray-500'
}

async function fetchDetail() {
  loading.value = true
  try {
    order.value = await orderAPI.detail(route.params.orderNo)
  } catch { order.value = null }
  loading.value = false
}

function handlePay() {
  router.push(`/pay?orderNo=${order.value.orderNo}`)
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定取消此订单吗？', '确认', { type: 'warning' })
  } catch { return }
  try {
    await orderAPI.cancel(order.value.orderNo)
    ElMessage.success('订单已取消')
    fetchDetail()
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}

async function handleShip() {
  try {
    await ElMessageBox.confirm('确认订单已发货？', '确认', { type: 'info' })
  } catch { return }
  try {
    await orderAPI.updateStatus(order.value.orderNo, 2)
    ElMessage.success('已标记为发货')
    fetchDetail()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleComplete() {
  try {
    await ElMessageBox.confirm('确认订单已完成？', '确认', { type: 'info' })
  } catch { return }
  try {
    await orderAPI.updateStatus(order.value.orderNo, 3)
    ElMessage.success('已标记为完成')
    fetchDetail()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => fetchDetail())
</script>
