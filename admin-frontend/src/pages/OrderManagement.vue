<template>
  <div class="space-y-4 animate-fade-in">
    <div class="bg-white rounded-xl border border-gray-100 p-5 flex items-center justify-between">
      <div>
        <h3 class="font-semibold text-gray-800 mb-1">订单管理</h3>
        <p class="text-sm text-gray-500">查看和管理订单，待支付订单可直接跳转支付</p>
      </div>
      <div class="flex gap-3">
        <select v-model="statusFilter" @change="fetchOrders"
          class="px-3 py-2 border border-gray-200 rounded-lg text-sm bg-white focus:outline-none focus:border-indigo-400">
          <option :value="null">全部状态</option>
          <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
      </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-x-auto">
      <table class="w-full min-w-[600px]">
        <thead class="bg-gray-50 text-left">
          <tr>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">订单号</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">金额</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">状态</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">收货人</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">时间</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="o in orders" :key="o.orderNo" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-5 py-3 text-sm text-gray-800 font-mono">{{ String(o.orderNo).slice(-8) }}</td>
            <td class="px-5 py-3">
              <span class="font-medium text-gray-800">¥{{ ((o.payAmount != null ? o.payAmount : (o.totalAmount - o.discountAmount)) / 100).toFixed(2) }}</span>
              <span v-if="o.discountAmount" class="text-xs text-red-500 ml-1">-¥{{ (o.discountAmount / 100).toFixed(2) }}</span>
            </td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium" :class="statusClass(o.status)">{{ o.statusText || statusLabel(o.status) }}</span>
            </td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ o.receiverName }}</td>
            <td class="px-5 py-3 text-sm text-gray-500">{{ o.createTime }}</td>
            <td class="px-5 py-3">
              <div class="flex flex-wrap gap-1.5">
                <button @click="$router.push(`/orders/${o.orderNo}`)"
                  class="text-xs px-2.5 py-1.5 rounded-md bg-gray-100 text-gray-600 hover:bg-gray-200 transition-colors whitespace-nowrap">详情</button>
                <button v-if="o.status === 0" @click="handlePay(o)"
                  class="text-xs px-2.5 py-1.5 rounded-md bg-indigo-500 text-white hover:bg-indigo-600 transition-colors whitespace-nowrap font-medium">去支付</button>
                <button v-if="o.status === 0" @click="handleCancel(o)"
                  class="text-xs px-2.5 py-1.5 rounded-md bg-red-50 text-red-600 hover:bg-red-100 transition-colors whitespace-nowrap">取消</button>
                <button v-if="o.status === 1" @click="handleShip(o)"
                  class="text-xs px-2.5 py-1.5 rounded-md bg-green-500 text-white hover:bg-green-600 transition-colors whitespace-nowrap">发货</button>
                <button v-if="o.status === 2" @click="handleComplete(o)"
                  class="text-xs px-2.5 py-1.5 rounded-md bg-blue-500 text-white hover:bg-blue-600 transition-colors whitespace-nowrap">完成</button>
              </div>
            </td>
          </tr>
          <tr v-if="orders.length === 0 && !loading">
            <td colspan="6" class="px-5 py-12 text-center text-gray-400">暂无订单</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="total > pageSize" class="flex justify-center mt-4">
      <el-pagination
        background layout="prev, pager, next"
        :total="total" :page-size="pageSize"
        v-model:current-page="page" @current-change="fetchOrders"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderAPI } from '@/api'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const pageSize = 20
const statusFilter = ref(null)

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

async function fetchOrders() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize }
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    const data = await orderAPI.myOrders(params)
    orders.value = data?.records || data?.list || []
    total.value = data?.total || 0
  } catch { orders.value = [] }
  loading.value = false
}

function handlePay(o) {
  router.push(`/pay?orderNo=${o.orderNo}`)
}

async function handleCancel(o) {
  try {
    await ElMessageBox.confirm(`确定取消订单 #${String(o.orderNo).slice(-8)} 吗？`, '确认', { type: 'warning' })
  } catch { return }
  try {
    await orderAPI.cancel(o.orderNo)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}

async function handleShip(o) {
  try {
    await ElMessageBox.confirm(`确认订单 #${String(o.orderNo).slice(-8)} 已发货？`, '确认', { type: 'info' })
  } catch { return }
  try {
    await orderAPI.updateStatus(o.orderNo, 2)
    ElMessage.success('已标记为发货')
    fetchOrders()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleComplete(o) {
  try {
    await ElMessageBox.confirm(`确认订单 #${String(o.orderNo).slice(-8)} 已完成？`, '确认', { type: 'info' })
  } catch { return }
  try {
    await orderAPI.updateStatus(o.orderNo, 3)
    ElMessage.success('已标记为完成')
    fetchOrders()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => fetchOrders())
</script>
