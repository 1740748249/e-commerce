<template>
  <div v-if="auth.isVendor" class="px-4 md:px-0">
    <router-link to="/vendor" class="md:hidden text-gray-400 active:text-gray-600 text-xl">&larr;</router-link>
    <h1 class="text-lg md:text-2xl font-bold text-gray-800 mt-2 mb-6">秒杀管理</h1>

    <!-- 可报名场次 -->
    <div class="bg-white rounded-2xl p-5 md:p-6 shadow-md mb-6">
      <h2 class="text-base md:text-lg font-semibold text-gray-800 mb-4">可报名场次</h2>
      <div v-if="sessions.length === 0" class="text-center py-8 text-gray-400 text-sm">
        暂无可报名的秒杀场次
      </div>
      <div v-else class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="s in sessions"
          :key="s.id"
          class="border rounded-xl p-4 hover:border-orange-300 hover:shadow-md transition-all"
        >
          <div class="flex items-center justify-between mb-2">
            <span class="font-semibold text-gray-800">{{ s.name }}</span>
            <span :class="sessionStatusClass(s.status)" class="text-xs px-2 py-0.5 rounded-full font-medium">
              {{ s.statusText || statusLabel(s.status) }}
            </span>
          </div>
          <div class="text-xs text-gray-400 space-y-1 mb-4">
            <p>开始：{{ fmt(s.startTime) }}</p>
            <p>结束：{{ fmt(s.endTime) }}</p>
            <p>已报名：{{ s.itemCount || 0 }} 件商品</p>
          </div>
          <button
            @click="openApply(s)"
            :disabled="s.status !== 0"
            class="w-full py-2 rounded-xl text-sm font-medium text-white bg-gradient-to-r from-orange-500 to-amber-500 hover:shadow-lg hover:shadow-orange-200 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {{ s.status === 0 ? '立即报名' : s.status === 1 ? '已开始' : '已结束' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 我的报名记录 -->
    <div class="bg-white rounded-2xl p-5 md:p-6 shadow-md">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <h2 class="text-base md:text-lg font-semibold text-gray-800">我的报名记录</h2>
        <div class="flex items-center gap-2 flex-wrap">
          <button
            v-for="opt in statusFilterOptions"
            :key="opt.value"
            @click="filterStatus = opt.value; appPage = 1; fetchApplications()"
            :class="[
              'text-xs px-3 py-1 rounded-full font-medium transition-colors',
              filterStatus === opt.value
                ? 'bg-orange-100 text-orange-600'
                : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
            ]"
          >{{ opt.label }}</button>
        </div>
      </div>
      <div v-if="applications.length === 0" class="text-center py-12 text-gray-400 text-sm">
        暂无报名记录，去上方场次报名吧
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="a in applications"
          :key="a.id"
          class="flex flex-col sm:flex-row sm:items-center gap-3 p-4 bg-gray-50 rounded-xl"
        >
          <div class="flex items-center gap-3 flex-1 min-w-0">
            <img
              v-if="a.productImage"
              :src="a.productImage"
              class="w-14 h-14 rounded-lg object-cover shrink-0"
            />
            <div class="min-w-0 flex-1">
              <p class="text-sm font-medium text-gray-800 truncate">{{ a.productName }}</p>
              <p class="text-xs text-gray-400 mt-0.5">{{ a.sessionName }}</p>
              <div class="flex items-center gap-2 mt-1 flex-wrap">
                <span class="text-xs font-bold text-red-500">秒杀价 ¥{{ cents(a.flashPrice) }}</span>
                <span class="text-xs text-gray-400 line-through">原价 ¥{{ cents(a.originalPrice) }}</span>
                <span class="text-xs text-gray-400">库存 {{ a.stock }}</span>
                <span class="text-xs text-gray-400">每人限购 {{ a.perUserLimit || 1 }} 件</span>
              </div>
            </div>
          </div>
          <div class="flex sm:flex-col items-center sm:items-end justify-between gap-2 shrink-0">
            <span :class="approvalClass(a.approvalStatus)" class="text-xs px-2.5 py-1 rounded-full font-medium whitespace-nowrap">
              {{ a.approvalStatusText || approvalLabel(a.approvalStatus) }}
            </span>
            <span class="text-xs text-gray-300">{{ fmt(a.createTime) }}</span>
          </div>
          <p v-if="a.rejectReason" class="text-xs text-red-400 bg-red-50 rounded-lg px-3 py-1.5 sm:ml-17">{{ a.rejectReason }}</p>
        </div>
      </div>
      <div v-if="appTotal > appSize" class="flex items-center justify-center gap-2 mt-6">
        <button
          @click="appPage--; fetchApplications()"
          :disabled="appPage <= 1"
          class="px-3 py-1.5 text-sm rounded-lg border hover:border-orange-300 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
        >上一页</button>
        <span class="text-sm text-gray-500">{{ appPage }} / {{ Math.ceil(appTotal / appSize) }}</span>
        <button
          @click="appPage++; fetchApplications()"
          :disabled="appPage >= Math.ceil(appTotal / appSize)"
          class="px-3 py-1.5 text-sm rounded-lg border hover:border-orange-300 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
        >下一页</button>
      </div>
    </div>

    <!-- 报名弹窗 -->
    <el-dialog v-model="dialogVisible" title="报名秒杀" width="440px" destroy-on-close>
      <div class="space-y-4">
        <div>
          <label class="text-sm font-medium text-gray-700">秒杀场次</label>
          <p class="text-sm text-gray-500 mt-1">{{ currentSession?.name }}</p>
        </div>
        <div>
          <label class="text-sm font-medium text-gray-700">选择商品</label>
          <el-select v-model="form.productId" placeholder="请选择商品" class="w-full mt-1" filterable @change="onProductChange">
            <el-option
              v-for="p in myProducts"
              :key="p.id"
              :label="`${p.name}（¥${cents(p.price)} / 库存${p.stock}）`"
              :value="p.id"
            />
          </el-select>
        </div>
        <div v-if="selectedProduct && selectedProduct.skus && selectedProduct.skus.length">
          <label class="text-sm font-medium text-gray-700">选择规格</label>
          <el-select v-model="form.skuId" placeholder="请选择参与秒杀的规格" class="w-full mt-1" filterable>
            <el-option
              v-for="s in selectedProduct.skus"
              :key="s.id"
              :label="`${skuLabel(s)}（¥${cents(s.price)} / 库存${s.stock}）`"
              :value="s.id"
            />
          </el-select>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-sm font-medium text-gray-700">秒杀价（元）</label>
            <el-input-number v-model="form.flashPriceYuan" :min="0.01" :precision="2" class="w-full mt-1" placeholder="0.00" />
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">秒杀库存</label>
            <el-input-number v-model="form.stock" :min="1" :max="9999" class="w-full mt-1" placeholder="1" />
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">每人限购</label>
            <el-input-number v-model="form.perUserLimit" :min="1" :max="99" class="w-full mt-1" placeholder="1" />
          </div>
        </div>
        <div v-if="selectedProduct" class="text-xs text-gray-400 bg-gray-50 rounded-lg p-3">
          原价 ¥{{ cents(selectedProduct.price) }}，库存 {{ selectedProduct.stock }}
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply" :loading="submitting">确认报名</el-button>
      </template>
    </el-dialog>
  </div>

  <div v-else class="text-center py-24">
    <p class="text-gray-500 font-medium text-lg">商家专属后台</p>
    <p class="text-sm text-gray-400 mt-1">请使用商家账号登录</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useProductStore } from '@/stores/product'
import { merchantFlashSessionAPI, merchantFlashSaleAPI } from '@/api'

const auth = useAuthStore()
const productStore = useProductStore()

const sessions = ref([])
const applications = ref([])
const myProducts = ref([])

const appPage = ref(1)
const appSize = ref(20)
const appTotal = ref(0)
const filterStatus = ref(null)

const statusFilterOptions = [
  { label: '全部', value: null },
  { label: '待审核', value: 0 },
  { label: '已通过', value: 1 },
  { label: '已拒绝', value: 2 },
]

const dialogVisible = ref(false)
const submitting = ref(false)
const currentSession = ref(null)
const form = ref({ productId: null, skuId: null, flashPriceYuan: 1, stock: 1, perUserLimit: 1 })

const selectedProduct = computed(() =>
  myProducts.value.find(p => p.id === form.value.productId) || null
)

onMounted(async () => {
  await Promise.all([
    fetchSessions(),
    fetchApplications(),
    fetchMyProducts(),
  ])
})

async function fetchSessions() {
  try { sessions.value = await merchantFlashSessionAPI.available() || [] } catch { /* */ }
}

async function fetchApplications() {
  try {
    const params = { page: appPage.value, size: appSize.value }
    if (filterStatus.value != null) params.approvalStatus = filterStatus.value
    const data = await merchantFlashSaleAPI.myApplications(params)
    applications.value = data?.list || []
    appTotal.value = data?.total || 0
    appSize.value = data?.size || 20
  } catch { /* */ }
}

async function fetchMyProducts() {
  try {
    const data = await productStore.fetchMyProducts()
    myProducts.value = productStore.list || []
  } catch { myProducts.value = [] }
}

function openApply(session) {
  currentSession.value = session
  form.value = { productId: null, skuId: null, flashPriceYuan: 1, stock: 1, perUserLimit: 1 }
  dialogVisible.value = true
}

function onProductChange() {
  form.value.skuId = null
}

async function submitApply() {
  if (!form.value.productId) {
    ElMessage.warning('请选择商品')
    return
  }
  if (!form.value.skuId) {
    ElMessage.warning('请选择参与秒杀的规格')
    return
  }
  submitting.value = true
  try {
    await merchantFlashSaleAPI.apply({
      sessionId: currentSession.value.id,
      productId: form.value.productId,
      skuId: form.value.skuId,
      flashPrice: Math.round(form.value.flashPriceYuan * 100),
      stock: form.value.stock,
      perUserLimit: form.value.perUserLimit,
    })
    ElMessage.success('报名成功，等待审核')
    dialogVisible.value = false
    appPage.value = 1
    await fetchApplications()
    await fetchSessions()
  } catch (e) {
    ElMessage.error(e.message || '报名失败')
  } finally {
    submitting.value = false
  }
}

function fmt(t) { return t ? new Date(t).toLocaleString('zh-CN') : '-' }
function cents(v) { return v != null ? (v / 100).toFixed(2) : '-' }

function statusLabel(s) { return s === 0 ? '未开始' : s === 1 ? '进行中' : '已结束' }
function sessionStatusClass(s) {
  return s === 0 ? 'bg-blue-100 text-blue-700' : s === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
}

function skuLabel(sku) {
  const specs = Array.isArray(sku.specs)
    ? sku.specs
    : (sku.specs && typeof sku.specs === 'object' ? Object.entries(sku.specs).map(([k, v]) => ({ name: k, value: v })) : [])
  return specs.map(sp => sp.value).join(' / ') || '默认'
}

function approvalLabel(s) {
  if (s === 0 || s === 'PENDING') return '待审核'
  if (s === 1 || s === 'APPROVED') return '已通过'
  if (s === 2 || s === 'REJECTED') return '已拒绝'
  return String(s)
}
function approvalClass(s) {
  if (s === 0 || s === 'PENDING') return 'bg-yellow-100 text-yellow-700'
  if (s === 1 || s === 'APPROVED') return 'bg-green-100 text-green-700'
  if (s === 2 || s === 'REJECTED') return 'bg-red-100 text-red-700'
  return 'bg-gray-100 text-gray-500'
}
</script>
