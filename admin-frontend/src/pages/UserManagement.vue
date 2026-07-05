<template>
  <div class="space-y-4 animate-fade-in">
    <!-- 筛选栏 -->
    <div class="bg-white rounded-xl border border-gray-100 p-5 space-y-4">
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h3 class="font-semibold text-gray-800">用户管理</h3>
          <p class="text-sm text-gray-500">管理系统内所有用户账号状态</p>
        </div>
      </div>

      <!-- 搜索 + 筛选行 -->
      <div class="flex flex-wrap items-center gap-3">
        <!-- 搜索框 -->
        <div class="flex-1 min-w-[200px]">
          <input
            v-model="filters.keyword"
            type="text"
            placeholder="搜索用户名 / 姓名 / 手机号"
            class="w-full px-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
            @keyup.enter="search"
          />
        </div>

        <!-- 状态筛选 -->
        <select v-model="filters.status"
          class="px-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-indigo-500">
          <option value="">全部状态</option>
          <option value="1">正常</option>
          <option value="0">已禁用</option>
        </select>

        <!-- 角色筛选 -->
        <select v-model="filters.role"
          class="px-4 py-2 rounded-lg border border-gray-300 text-sm outline-none focus:ring-2 focus:ring-indigo-500">
          <option value="">全部角色</option>
          <option value="0">普通用户</option>
          <option value="1">商家</option>
          <option value="2">管理员</option>
        </select>

        <!-- 时间范围 -->
        <div class="flex items-center gap-2">
          <el-date-picker v-model="filters.startTime" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width:140px" />
          <span class="text-gray-400 text-sm shrink-0">至</span>
          <el-date-picker v-model="filters.endTime" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width:140px" />
        </div>

        <!-- 操作按钮 -->
        <button @click="search"
          class="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition-colors">
          搜索
        </button>
        <button @click="reset"
          class="px-4 py-2 border border-gray-300 hover:bg-gray-50 text-sm font-medium rounded-lg transition-colors">
          重置
        </button>
      </div>
    </div>

    <!-- 用户表格 -->
    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 text-left">
          <tr>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">用户名</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">姓名</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">手机号</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">角色</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">状态</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">最后登录</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500 text-right">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="u in userList" :key="u.id" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-5 py-3">
              <span class="font-medium text-gray-800">{{ u.username }}</span>
            </td>
            <td class="px-5 py-3 text-sm text-gray-700">{{ u.name }}</td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ u.phone }}</td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                :class="roleClass(u.role)">
                {{ roleText(u.role) }}
              </span>
            </td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                :class="u.status === 1 ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'">
                {{ u.status === 1 ? '正常' : '已禁用' }}
              </span>
            </td>
            <td class="px-5 py-3 text-sm text-gray-500">{{ u.lastLoginTime || u.createTime || '-' }}</td>
            <td class="px-5 py-3 text-right">
              <button @click="toggleStatus(u)"
                class="px-3 py-1.5 text-sm font-medium rounded-lg transition-colors"
                :class="u.status === 1
                  ? 'bg-red-50 hover:bg-red-100 text-red-600'
                  : 'bg-green-50 hover:bg-green-100 text-green-600'">
                {{ u.status === 1 ? '禁用' : '启用' }}
              </button>
            </td>
          </tr>
          <!-- 空状态 -->
          <tr v-if="userList.length === 0 && !loading">
            <td colspan="7" class="px-5 py-12 text-center text-gray-400">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="flex items-center justify-between bg-white rounded-xl border border-gray-100 px-5 py-3">
      <span class="text-sm text-gray-500">共 {{ total }} 条</span>
      <div class="flex items-center gap-2">
        <button :disabled="page <= 1" @click="goPage(page - 1)"
          class="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40 hover:bg-gray-50 transition-colors">
          上一页
        </button>
        <span class="text-sm text-gray-700">{{ page }} / {{ totalPages || 1 }}</span>
        <button :disabled="page >= totalPages" @click="goPage(page + 1)"
          class="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40 hover:bg-gray-50 transition-colors">
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminAPI } from '@/api'

const loading = ref(false)
const userList = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const totalPages = ref(0)

const filters = reactive({
  keyword: '',
  status: '',
  role: '',
  startTime: '',
  endTime: '',
})

function roleText(role) {
  const map = { 0: '用户', 1: '商家', 2: '管理员' }
  return map[role] || '未知'
}

function roleClass(role) {
  if (role === 1) return 'bg-purple-50 text-purple-700'
  if (role === 2) return 'bg-orange-50 text-orange-700'
  return 'bg-blue-50 text-blue-700'
}

function buildParams() {
  const params = { page: page.value, size: size.value }
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.status !== '') params.status = filters.status
  if (filters.role !== '') params.role = filters.role
  if (filters.startTime) params.startTime = filters.startTime + ' 00:00:00'
  if (filters.endTime) params.endTime = filters.endTime + ' 23:59:59'
  return params
}

async function load() {
  loading.value = true
  try {
    const data = await adminAPI.getUsers(buildParams())
    userList.value = data.list || []
    total.value = data.total || 0
    totalPages.value = data.pages || 0
  } catch (e) {
    console.error('加载用户列表失败:', e)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.status = ''
  filters.role = ''
  filters.startTime = ''
  filters.endTime = ''
  page.value = 1
  load()
}

function goPage(p) {
  page.value = p
  load()
}

async function toggleStatus(user) {
  const newStatus = user.status === 1 ? 0 : 1
  const prevStatus = user.status
  user.status = newStatus
  try {
    await adminAPI.updateUserStatus(user.id, newStatus)
  } catch (e) {
    user.status = prevStatus
    console.error('更新用户状态失败:', e)
  }
}

onMounted(load)
</script>

<style scoped>
:deep(.el-date-editor) {
  --el-input-border-radius: 8px;
}
:deep(.el-date-editor .el-input__wrapper) {
  border-color: #d1d5db;
  box-shadow: none;
  padding: 4px 12px;
}
:deep(.el-date-editor .el-input__wrapper:hover) {
  border-color: #9ca3af;
}
:deep(.el-date-editor.is-focus .el-input__wrapper) {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.25);
}
:deep(.el-date-editor .el-input__inner) {
  font-size: 0.875rem;
  color: #374151;
}
:deep(.el-date-editor .el-input__inner::placeholder) {
  color: #9ca3af;
}
</style>
