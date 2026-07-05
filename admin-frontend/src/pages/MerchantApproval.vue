<template>
  <div class="space-y-4 animate-fade-in">
    <div class="bg-white rounded-xl border border-gray-100 p-5">
      <h3 class="font-semibold text-gray-800 mb-1">待审批商家</h3>
      <p class="text-sm text-gray-500">审核商家注册申请，批准后商家即可登录并管理店铺</p>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div v-if="pending.length === 0 && !loading" class="p-12 text-center text-gray-400">
        <span class="text-4xl block mb-3">✅</span>
        <p>没有待审批的商家</p>
      </div>

      <table v-else class="w-full">
        <thead class="bg-gray-50 text-left">
          <tr>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">商家</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">店铺名称</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">手机号</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">注册时间</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500 text-right">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="m in pending" :key="m.shopId" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-5 py-3">
              <p class="font-medium text-gray-800">{{ m.userName }}</p>
            </td>
            <td class="px-5 py-3 text-sm text-gray-700">{{ m.shopName }}</td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ m.userPhone }}</td>
            <td class="px-5 py-3 text-sm text-gray-500">{{ m.createTime }}</td>
            <td class="px-5 py-3 text-right">
              <div class="flex items-center justify-end gap-2">
                <button @click="handleApprove(m)"
                  :disabled="processingId === m.shopId"
                  class="px-3 py-1.5 bg-green-500 hover:bg-green-600 disabled:bg-green-300 text-white text-sm font-medium rounded-lg transition-colors">
                  批准
                </button>
                <button @click="showRejectModal(m)"
                  :disabled="processingId === m.shopId"
                  class="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 text-sm font-medium rounded-lg transition-colors">
                  拒绝
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 拒绝确认弹窗 -->
    <div v-if="rejectTarget" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="rejectTarget = null">
      <div class="bg-white rounded-xl p-6 w-full max-w-sm shadow-xl">
        <h4 class="font-semibold text-gray-800 mb-2">确认拒绝</h4>
        <p class="text-sm text-gray-500 mb-4">
          确定拒绝「<strong>{{ rejectTarget.userName }}</strong>」的商家注册申请吗？此操作不可恢复。
        </p>
        <div class="flex gap-3 justify-end">
          <button @click="rejectTarget = null"
            class="px-4 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors">取消</button>
          <button @click="handleReject"
            class="px-4 py-2 text-sm bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors">确认拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminAPI } from '@/api'

const pending = ref([])
const processingId = ref(null)
const rejectTarget = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await adminAPI.getPendingShops({ page: 1, size: 50 })
    pending.value = data.list || []
  } catch (e) {
    console.error('加载待审批商家失败:', e)
  } finally {
    loading.value = false
  }
}
onMounted(load)

function showRejectModal(merchant) {
  rejectTarget.value = merchant
}

async function handleApprove(merchant) {
  processingId.value = merchant.shopId
  try {
    await adminAPI.approveShop(merchant.shopId, true)
    pending.value = pending.value.filter(m => m.shopId !== merchant.shopId)
  } catch (e) {
    console.error('审批失败:', e)
  } finally {
    processingId.value = null
  }
}

async function handleReject() {
  if (!rejectTarget.value) return
  const id = rejectTarget.value.shopId
  try {
    await adminAPI.approveShop(id, false)
    pending.value = pending.value.filter(m => m.shopId !== id)
  } finally {
    rejectTarget.value = null
  }
}
</script>
