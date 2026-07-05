<template>
  <div class="space-y-4 animate-fade-in">
    <div class="bg-white rounded-xl border border-gray-100 p-5 flex items-center justify-between">
      <div>
        <h3 class="font-semibold text-gray-800 mb-1">优惠券管理</h3>
        <p class="text-sm text-gray-500">创建和管理平台优惠券，用户可在前台领取和使用</p>
      </div>
      <button @click="openCreate" class="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition-colors">+ 新建优惠券</button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 text-left">
          <tr>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">ID</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">名称</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">类型</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">门槛/减免</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">有效期</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">领取进度</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">状态</th>
            <th class="px-5 py-3 text-sm font-medium text-gray-500">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="c in coupons" :key="c.id" class="hover:bg-gray-50/50 transition-colors">
            <td class="px-5 py-3 text-sm text-gray-600">{{ c.id }}</td>
            <td class="px-5 py-3"><span class="font-medium text-gray-800">{{ c.name }}</span></td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium" :class="c.type === 1 ? 'bg-green-50 text-green-700' : 'bg-blue-50 text-blue-700'">
                {{ c.type === 1 ? '无门槛券' : '满减券' }}
              </span>
            </td>
            <td class="px-5 py-3 text-sm text-gray-600">
              <template v-if="c.type === 0">满 ¥{{ (c.threshold / 100).toFixed(0) }} 减 ¥{{ (c.reduce / 100).toFixed(0) }}</template>
              <template v-else>立减 ¥{{ (c.reduce / 100).toFixed(0) }}</template>
            </td>
            <td class="px-5 py-3 text-sm text-gray-600">{{ c.validDays }} 天</td>
            <td class="px-5 py-3 text-sm text-gray-600">
              <div class="flex items-center gap-1.5">
                <div class="w-20 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div class="h-full bg-indigo-500 rounded-full" :style="{ width: claimPercent(c) + '%' }"></div>
                </div>
                <span>{{ c.claimedCount }} / {{ c.totalCount }}</span>
              </div>
            </td>
            <td class="px-5 py-3">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium" :class="c.status === 1 ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-500'">
                {{ c.status === 1 ? '启用' : '停用' }}
              </span>
            </td>
            <td class="px-5 py-3">
              <div class="flex gap-2">
                <button @click="openEdit(c)" class="text-xs px-3 py-1.5 rounded-md bg-gray-100 text-gray-600 hover:bg-gray-200 transition-colors">编辑</button>
                <button
                  @click="toggleStatus(c)"
                  :class="c.status === 1
                    ? 'text-xs px-3 py-1.5 rounded-md bg-yellow-50 text-yellow-700 hover:bg-yellow-100 transition-colors'
                    : 'text-xs px-3 py-1.5 rounded-md bg-green-50 text-green-700 hover:bg-green-100 transition-colors'"
                >
                  {{ c.status === 1 ? '停用' : '启用' }}
                </button>
                <button @click="handleDelete(c)" class="text-xs px-3 py-1.5 rounded-md bg-red-50 text-red-600 hover:bg-red-100 transition-colors">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="coupons.length === 0 && !loading">
            <td colspan="8" class="px-5 py-12 text-center text-gray-400">暂无优惠券</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="flex justify-center mt-4">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="page"
        @current-change="fetchCoupons"
      />
    </div>

    <!-- 创建/编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/30" @click="showModal = false"></div>
        <div class="relative bg-white rounded-2xl p-6 w-full max-w-md shadow-xl mx-4 max-h-[85vh] overflow-y-auto">
          <h4 class="font-semibold text-gray-800 mb-4">{{ editingId ? '编辑优惠券' : '新建优惠券' }}</h4>
          <div class="space-y-3">
            <label class="block">
              <span class="text-sm text-gray-500 block mb-1.5">名称 <span class="text-red-400">*</span></span>
              <input v-model="form.name" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400" placeholder="如：新用户满减券" />
            </label>
            <label class="block">
              <span class="text-sm text-gray-500 block mb-1.5">类型 <span class="text-red-400">*</span></span>
              <select v-model="form.type" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400 bg-white">
                <option :value="0">满减券</option>
                <option :value="1">无门槛券</option>
              </select>
            </label>
            <template v-if="form.type === 0">
              <label class="block">
                <span class="text-sm text-gray-500 block mb-1.5">使用门槛（元）<span class="text-red-400">*</span></span>
                <input v-model.number="thresholdYuan" type="number" min="1" step="0.01" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400" placeholder="满多少可以使用" />
              </label>
            </template>
            <label class="block">
              <span class="text-sm text-gray-500 block mb-1.5">减免金额（元）<span class="text-red-400">*</span></span>
              <input v-model.number="reduceYuan" type="number" min="0.01" step="0.01" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400" placeholder="减多少" />
            </label>
            <label class="block">
              <span class="text-sm text-gray-500 block mb-1.5">有效天数 <span class="text-red-400">*</span></span>
              <input v-model.number="form.validDays" type="number" min="1" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400" placeholder="领取后有效天数" />
            </label>
            <label class="block">
              <span class="text-sm text-gray-500 block mb-1.5">每人限领 <span class="text-red-400">*</span></span>
              <input v-model.number="form.limitPerUser" type="number" min="1" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400" placeholder="每人最多领几张" />
            </label>
            <label class="block">
              <span class="text-sm text-gray-500 block mb-1.5">总发行量 <span class="text-red-400">*</span></span>
              <input v-model.number="form.totalCount" type="number" min="1" class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-indigo-400" placeholder="一共发多少张" />
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
import { couponAdminAPI } from '@/api'

const coupons = ref([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const pageSize = 20

const showModal = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const form = ref({ name: '', type: 0, threshold: 0, reduce: 0, validDays: 7, limitPerUser: 1, totalCount: 100 })
const thresholdYuan = ref(0)
const reduceYuan = ref(0)

function claimPercent(c) {
  if (!c.totalCount) return 0
  return Math.min(100, Math.round((c.claimedCount / c.totalCount) * 100))
}

async function fetchCoupons() {
  loading.value = true
  try {
    const data = await couponAdminAPI.list({ page: page.value, size: pageSize })
    coupons.value = data?.records || data?.list || []
    total.value = data?.total || 0
  } catch { coupons.value = [] }
  loading.value = false
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', type: 0, threshold: 0, reduce: 0, validDays: 7, limitPerUser: 1, totalCount: 100 }
  thresholdYuan.value = 0
  reduceYuan.value = 0
  showModal.value = true
}

function openEdit(c) {
  editingId.value = c.id
  form.value = {
    name: c.name,
    type: c.type,
    validDays: c.validDays,
    limitPerUser: c.limitPerUser,
    totalCount: c.totalCount,
  }
  thresholdYuan.value = c.threshold ? c.threshold / 100 : 0
  reduceYuan.value = c.reduce ? c.reduce / 100 : 0
  showModal.value = true
}

async function submit() {
  if (!form.value.name) { ElMessage.warning('请输入优惠券名称'); return }
  if (form.value.type === 0 && thresholdYuan.value <= 0) { ElMessage.warning('满减券必须设置使用门槛'); return }
  if (reduceYuan.value <= 0) { ElMessage.warning('减免金额必须大于0'); return }
  if (!form.value.validDays || form.value.validDays < 1) { ElMessage.warning('有效天数必须大于0'); return }
  if (!form.value.limitPerUser || form.value.limitPerUser < 1) { ElMessage.warning('每人限领必须大于0'); return }
  if (!form.value.totalCount || form.value.totalCount < 1) { ElMessage.warning('总发行量必须大于0'); return }

  const body = {
    name: form.value.name,
    type: form.value.type,
    threshold: Math.round(thresholdYuan.value * 100),
    reduce: Math.round(reduceYuan.value * 100),
    validDays: form.value.validDays,
    limitPerUser: form.value.limitPerUser,
    totalCount: form.value.totalCount,
  }

  submitting.value = true
  try {
    if (editingId.value) {
      await couponAdminAPI.update(editingId.value, body)
      ElMessage.success('编辑成功')
      const c = coupons.value.find(c => c.id === editingId.value)
      if (c) Object.assign(c, { ...body, threshold: body.threshold, reduce: body.reduce })
    } else {
      const created = await couponAdminAPI.create(body)
      ElMessage.success('创建成功')
      coupons.value.unshift(created || { id: Date.now(), ...body, status: 1, claimedCount: 0 })
    }
    showModal.value = false
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
  submitting.value = false
}

async function toggleStatus(c) {
  const newStatus = c.status === 1 ? 0 : 1
  const label = newStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${label}优惠券「${c.name}」吗？`, '确认', { type: 'warning' })
  } catch { return }
  const prevStatus = c.status
  c.status = newStatus
  try {
    await couponAdminAPI.updateStatus(c.id, newStatus)
    ElMessage.success(`已${label}`)
  } catch (e) {
    c.status = prevStatus
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(c) {
  try {
    await ElMessageBox.confirm(`确定删除优惠券「${c.name}」吗？`, '确认', { type: 'warning' })
  } catch { return }
  const idx = coupons.value.findIndex(x => x.id === c.id)
  try {
    await couponAdminAPI.remove(c.id)
    ElMessage.success('已删除')
    coupons.value.splice(idx, 1)
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => fetchCoupons())
</script>
