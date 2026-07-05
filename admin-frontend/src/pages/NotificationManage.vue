<template>
  <div class="space-y-4 animate-fade-in">
    <div class="bg-white rounded-xl border border-gray-100 p-5">
      <h3 class="font-semibold text-gray-800 mb-1">通知管理</h3>
      <p class="text-sm text-gray-500">向指定店铺发送系统通知或促销活动通知，商家将在后台实时收到推送</p>
    </div>

    <!-- 全站广播 -->
    <div class="bg-white rounded-xl border border-gray-100 p-6 mb-4">
      <div class="flex items-center gap-2 mb-1">
        <span class="text-lg">📢</span>
        <h4 class="text-sm font-semibold text-gray-700">全站广播</h4>
      </div>
      <p class="text-xs text-gray-400 mb-5">推送给所有在线商家，同时落库保留记录，离线商家上线后可查看</p>
      <div class="max-w-xl space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1.5">广播标题</label>
          <el-input v-model="broadcastTitle" placeholder="例如：系统维护通知" maxlength="200" show-word-limit />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1.5">广播内容</label>
          <el-input v-model="broadcastContent" type="textarea" :rows="4" placeholder="输入广播内容..." maxlength="5000" show-word-limit />
        </div>
        <button
          @click="sendBroadcast"
          :disabled="broadcastSending || !broadcastTitle.trim() || !broadcastContent.trim()"
          class="px-6 py-2.5 bg-red-500 text-white text-sm rounded-lg hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {{ broadcastSending ? '发送中...' : '📢 发送全站广播' }}
        </button>
      </div>
    </div>

    <!-- 店铺通知 -->
    <div class="bg-white rounded-xl border border-gray-100 p-6">
      <h4 class="text-sm font-semibold text-gray-700 mb-5">店铺通知</h4>
      <div class="max-w-xl space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1.5">目标店铺</label>
          <el-select v-model="form.shopId" placeholder="请选择店铺" class="w-full" filterable>
            <el-option
              v-for="s in shops"
              :key="s.id"
              :label="`${s.name}（ID: ${s.id}）`"
              :value="s.id"
            />
          </el-select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1.5">通知类型</label>
          <el-select v-model="form.type" placeholder="请选择类型" class="w-full">
            <el-option label="系统通知" :value="1" />
            <el-option label="促销活动" :value="2" />
          </el-select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1.5">通知标题</label>
          <el-input v-model="form.title" placeholder="请输入标题（最长200字）" maxlength="200" show-word-limit />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1.5">通知内容</label>
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入通知内容（最长5000字）" maxlength="5000" show-word-limit />
        </div>
        <button
          @click="submit"
          :disabled="submitting"
          class="px-6 py-2.5 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {{ submitting ? '发送中...' : '发送通知' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { notificationAPI, shopAPI } from '@/api'

const shops = ref([])
const submitting = ref(false)
const form = ref({ shopId: null, type: 1, title: '', content: '' })

// ---- 广播 ----
const broadcastTitle = ref('')
const broadcastContent = ref('')
const broadcastSending = ref(false)

async function sendBroadcast() {
  broadcastSending.value = true
  try {
    await notificationAPI.broadcast(broadcastTitle.value.trim(), broadcastContent.value.trim())
    ElMessage.success('广播已发送')
    broadcastTitle.value = ''
    broadcastContent.value = ''
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    broadcastSending.value = false
  }
}

onMounted(async () => {
  try {
    shops.value = await shopAPI.list() || []
  } catch { /* */ }
})

async function submit() {
  if (!form.value.shopId) { ElMessage.warning('请选择目标店铺'); return }
  if (!form.value.title.trim()) { ElMessage.warning('请输入通知标题'); return }
  if (!form.value.content.trim()) { ElMessage.warning('请输入通知内容'); return }
  submitting.value = true
  try {
    await notificationAPI.send({
      shopId: form.value.shopId,
      type: form.value.type,
      title: form.value.title.trim(),
      content: form.value.content.trim(),
    })
    ElMessage.success('通知已发送')
    form.value.title = ''
    form.value.content = ''
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    submitting.value = false
  }
}
</script>
