<template>
  <div class="space-y-4 animate-fade-in">
    <div class="bg-white rounded-xl border border-gray-100 p-5">
      <h3 class="font-semibold text-gray-800 mb-1">秒杀报名审核</h3>
      <p class="text-sm text-gray-500">审核商家提交的秒杀报名申请，通过后场次时间到达时自动生效</p>
    </div>

    <!-- 筛选栏 -->
    <div class="bg-white rounded-xl border border-gray-100 p-4 flex items-center gap-4 flex-wrap">
      <select v-model="filterSessionId" @change="fetchList"
        class="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400">
        <option :value="null">全部场次</option>
        <option v-for="s in sessions" :key="s.id" :value="s.id">{{ s.name }}</option>
      </select>
      <select v-model="filterStatus" @change="fetchList"
        class="px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400">
        <option :value="null">全部状态</option>
        <option :value="0">待审核</option>
        <option :value="1">已通过</option>
        <option :value="2">已拒绝</option>
      </select>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 text-left">
          <tr>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">报名ID</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">场次</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">商品</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">商家</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">秒杀价</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">库存</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">每人限购</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">审核状态</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="a in list" :key="a.id" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-5 py-3 text-sm text-gray-600">{{ a.id }}</td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ a.sessionName }}</td>
            <td class="px-5 py-3">
              <div class="flex items-center gap-2">
                <img v-if="a.productImage" :src="a.productImage" class="w-8 h-8 rounded-lg object-cover" />
                <span class="text-sm text-gray-800">{{ a.productName }}</span>
              </div>
            </td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ a.shopName }}</td>
            <td class="px-5 py-3 text-sm text-red-500 font-medium">¥{{ ((a.flashPrice || 0) / 100).toFixed(0) }}</td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ a.stock }}</td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ a.perUserLimit || 1 }} 件</td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium" :class="approvalClass(a.approvalStatus)">
                {{ approvalLabel(a.approvalStatus) }}
              </span>
              <div v-if="a.approvalStatus === 2 && a.rejectReason" class="text-xs text-red-400 mt-0.5">{{ a.rejectReason }}</div>
            </td>
            <td class="px-5 py-3">
              <div v-if="a.approvalStatus === 0" class="flex gap-2">
                <button @click="handleApprove(a, true)"
                  class="text-xs px-3 py-1.5 rounded-md bg-green-50 text-green-600 hover:bg-green-100 transition-colors">通过</button>
                <button @click="openReject(a)"
                  class="text-xs px-3 py-1.5 rounded-md bg-red-50 text-red-600 hover:bg-red-100 transition-colors">拒绝</button>
              </div>
              <span v-else class="text-xs text-gray-400">-</span>
            </td>
          </tr>
          <tr v-if="list.length === 0 && !loading">
            <td colspan="8" class="px-5 py-12 text-center text-gray-400">暂无报名记录</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 拒绝弹窗 -->
    <Teleport to="body">
      <div v-if="showReject" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/30" @click="showReject = false"></div>
        <div class="relative bg-white rounded-2xl p-6 w-full max-w-md shadow-xl mx-4">
          <h4 class="font-semibold text-gray-800 mb-4">拒绝报名</h4>
          <label class="block">
            <span class="text-sm text-gray-500">拒绝原因</span>
            <textarea v-model="rejectReason" rows="3" placeholder="请填写拒绝原因"
              class="mt-1 w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-red-400 resize-none"></textarea>
          </label>
          <div class="flex justify-end gap-3 mt-5">
            <button @click="showReject = false" class="px-4 py-2 text-sm text-gray-500 hover:text-gray-700">取消</button>
            <button @click="submitReject" :disabled="!rejectReason.trim() || submitting"
              class="px-4 py-2 bg-red-600 text-white text-sm rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50">确认拒绝</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { flashSessionAPI, flashSaleApprovalAPI } from '@/api'

const list = ref([])
const sessions = ref([])
const loading = ref(false)
const filterSessionId = ref(null)
const filterStatus = ref(null)
const showReject = ref(false)
const rejectReason = ref('')
const submitting = ref(false)
const rejectTarget = ref(null)

function approvalLabel(s) {
  if (s === 0) return '待审核'
  if (s === 1) return '已通过'
  if (s === 2) return '已拒绝'
  return ''
}

function approvalClass(s) {
  if (s === 0) return 'bg-yellow-50 text-yellow-700'
  if (s === 1) return 'bg-green-50 text-green-700'
  return 'bg-red-50 text-red-700'
}

async function fetchSessions() {
  try {
    const data = await flashSessionAPI.list({ page: 1, size: 50 })
    sessions.value = data?.records || data?.list || []
  } catch { sessions.value = [] }
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: 1, size: 50 }
    if (filterSessionId.value) params.sessionId = filterSessionId.value
    if (filterStatus.value !== null) params.approvalStatus = filterStatus.value
    const data = await flashSaleApprovalAPI.applications(params)
    list.value = data?.records || data?.list || []
  } catch { list.value = [] }
  loading.value = false
}

async function handleApprove(item, approved) {
  const idx = list.value.findIndex(a => a.id === item.id)
  try {
    await flashSaleApprovalAPI.approve(item.id, { approved, rejectReason: null })
    ElMessage.success('已通过')
    list.value.splice(idx, 1)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

function openReject(item) {
  rejectTarget.value = item
  rejectReason.value = ''
  showReject.value = true
}

async function submitReject() {
  if (!rejectReason.value.trim()) return
  const idx = list.value.findIndex(a => a.id === rejectTarget.value.id)
  submitting.value = true
  try {
    await flashSaleApprovalAPI.approve(rejectTarget.value.id, { approved: false, rejectReason: rejectReason.value.trim() })
    ElMessage.success('已拒绝')
    showReject.value = false
    list.value.splice(idx, 1)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
  submitting.value = false
}

onMounted(() => {
  Promise.all([fetchSessions(), fetchList()])
})
</script>
