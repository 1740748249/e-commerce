<template>
  <div class="space-y-4 animate-fade-in">
    <div class="bg-white rounded-xl border border-gray-100 p-5 flex items-center justify-between">
      <div>
        <h3 class="font-semibold text-gray-800 mb-1">秒杀场次管理</h3>
        <p class="text-sm text-gray-500">创建和管理秒杀活动时间段，商家在有效场次内报名</p>
      </div>
      <button @click="openCreate" class="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors">+ 新建场次</button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 text-left">
          <tr>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">ID</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">场次名称</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">开始时间</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">结束时间</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">状态</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">报名数</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="s in sessions" :key="s.id" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-5 py-3 text-sm text-gray-600">{{ s.id }}</td>
            <td class="px-5 py-3"><span class="font-medium text-gray-800">{{ s.name }}</span></td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ s.startTime }}</td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ s.endTime }}</td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium" :class="statusClass(s.status)">
                {{ s.statusText || statusLabel(s.status) }}
              </span>
            </td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ s.itemCount || 0 }}</td>
            <td class="px-5 py-3 flex gap-2">
              <button :disabled="s.status !== 0" @click="openEdit(s)"
                class="text-xs px-3 py-1.5 rounded-md bg-gray-100 text-gray-600 hover:bg-gray-200 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">编辑</button>
              <button :disabled="s.status !== 0" @click="handleDelete(s)"
                class="text-xs px-3 py-1.5 rounded-md bg-red-50 text-red-600 hover:bg-red-100 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">删除</button>
            </td>
          </tr>
          <tr v-if="sessions.length === 0 && !loading">
            <td colspan="7" class="px-5 py-12 text-center text-gray-400">暂无秒杀场次</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 创建/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/30" @click="showModal = false"></div>
        <div class="relative bg-white rounded-2xl p-6 w-full max-w-md shadow-xl mx-4">
          <h4 class="font-semibold text-gray-800 mb-4">{{ editingId ? '编辑场次' : '新建场次' }}</h4>
          <div class="space-y-3">
            <label class="block">
              <span class="text-sm text-gray-500 block mb-1.5">场次名称</span>
              <input v-model="form.name" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400" placeholder="如：618秒杀专场" />
            </label>
            <label class="block">
              <span class="text-sm text-gray-500 block mb-1.5">开始时间</span>
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" class="w-full" />
            </label>
            <label class="block">
              <span class="text-sm text-gray-500 block mb-1.5">结束时间</span>
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" class="w-full" />
            </label>
          </div>
          <div class="flex justify-end gap-3 mt-5">
            <button @click="showModal = false" class="px-4 py-2 text-sm text-gray-500 hover:text-gray-700">取消</button>
            <button @click="submit" :disabled="submitting"
              class="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50">{{ submitting ? '提交中...' : '确认' }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { flashSessionAPI } from '@/api'

const sessions = ref([])
const loading = ref(false)
const showModal = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const form = ref({ name: '', startTime: '', endTime: '' })

function statusLabel(s) {
  if (s === 0) return '未开始'
  if (s === 1) return '进行中'
  if (s === 2) return '已结束'
  return ''
}

function statusClass(s) {
  if (s === 0) return 'bg-blue-50 text-blue-700'
  if (s === 1) return 'bg-green-50 text-green-700'
  return 'bg-gray-100 text-gray-500'
}

async function fetchSessions() {
  loading.value = true
  try {
    const data = await flashSessionAPI.list({ page: 1, size: 50 })
    sessions.value = data?.records || data?.list || []
  } catch { sessions.value = [] }
  loading.value = false
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', startTime: '', endTime: '' }
  showModal.value = true
}

function openEdit(s) {
  editingId.value = s.id
  form.value = { name: s.name, startTime: s.startTime, endTime: s.endTime }
  showModal.value = true
}

async function submit() {
  if (!form.value.name || !form.value.startTime || !form.value.endTime) {
    ElMessage.warning('请完整填写所有字段')
    return
  }
  const body = { name: form.value.name, startTime: form.value.startTime, endTime: form.value.endTime }
  submitting.value = true
  try {
    if (editingId.value) {
      await flashSessionAPI.update(editingId.value, body)
      ElMessage.success('编辑成功')
      const s = sessions.value.find(s => s.id === editingId.value)
      if (s) Object.assign(s, body)
    } else {
      const created = await flashSessionAPI.create(body)
      ElMessage.success('创建成功')
      sessions.value.unshift(created || { id: Date.now(), ...body, status: 0, itemCount: 0 })
    }
    showModal.value = false
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
  submitting.value = false
}

async function handleDelete(s) {
  try {
    await ElMessageBox.confirm(`确定删除场次「${s.name}」吗？`, '确认', { type: 'warning' })
  } catch { return }
  const idx = sessions.value.findIndex(x => x.id === s.id)
  try {
    await flashSessionAPI.remove(s.id)
    ElMessage.success('已删除')
    sessions.value.splice(idx, 1)
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => fetchSessions())
</script>

<style scoped>
:deep(.el-date-editor) {
  --el-input-border-radius: 8px;
}
:deep(.el-date-editor .el-input__wrapper) {
  border-color: #e5e7eb;
  box-shadow: none;
  padding: 4px 12px;
}
:deep(.el-date-editor .el-input__wrapper:hover) {
  border-color: #d1d5db;
}
:deep(.el-date-editor.is-focus .el-input__wrapper) {
  border-color: #818cf8;
  box-shadow: 0 0 0 1px #818cf8;
}
:deep(.el-date-editor .el-input__inner) {
  font-size: 0.875rem;
  color: #374151;
}
:deep(.el-date-editor .el-input__inner::placeholder) {
  color: #9ca3af;
}
</style>
