<template>
  <div v-if="order.loading" class="text-center py-20">
    <el-icon class="is-loading text-3xl text-orange-400"><span>&#8635;</span></el-icon>
    <p class="text-gray-400 text-sm mt-3">加载中...</p>
  </div>

  <div v-else-if="o" class="max-w-2xl mx-auto px-4 md:px-0 pb-24 md:pb-4">
    <!-- 顶部导航 -->
    <div class="flex items-center gap-3 mb-4">
      <button @click="$router.push('/user')" class="text-gray-400 hover:text-gray-600 flex items-center gap-1 transition-colors">
        &larr; 返回
      </button>
      <h1 class="text-lg font-bold text-gray-800">订单详情</h1>
    </div>

    <!-- ========== 移动端布局 ========== -->
    <div class="md:hidden space-y-3">
      <!-- 状态卡片 -->
      <div class="bg-gradient-to-br from-orange-500 to-amber-500 rounded-2xl p-4 text-white shadow-lg">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center text-lg shrink-0">
            {{ statusIcon }}
          </div>
          <div>
            <p class="text-white/70 text-xs">订单状态</p>
            <p class="text-lg font-bold">{{ statusText }}</p>
          </div>
        </div>
        <!-- 进度条 -->
        <div class="flex items-center mt-4 gap-0">
          <div class="flex flex-col items-center flex-1">
            <div class="w-6 h-6 rounded-full bg-white text-orange-500 text-[10px] flex items-center justify-center font-bold">&#10003;</div>
            <span class="text-[9px] text-white/70 mt-1">下单</span>
          </div>
          <div class="h-0.5 flex-1 mx-0.5" :class="progress >= 2 ? 'bg-white' : 'bg-white/30'"></div>
          <div class="flex flex-col items-center flex-1">
            <div class="w-6 h-6 rounded-full text-[10px] flex items-center justify-center font-bold" :class="progress >= 2 ? 'bg-white text-orange-500' : 'bg-white/30 text-white/50'">
              {{ progress >= 2 ? '&#10003;' : '2' }}
            </div>
            <span class="text-[9px] text-white/70 mt-1">发货</span>
          </div>
          <div class="h-0.5 flex-1 mx-0.5" :class="progress >= 3 ? 'bg-white' : 'bg-white/30'"></div>
          <div class="flex flex-col items-center flex-1">
            <div class="w-6 h-6 rounded-full text-[10px] flex items-center justify-center font-bold" :class="progress >= 3 ? 'bg-white text-orange-500' : 'bg-white/30 text-white/50'">
              {{ progress >= 3 ? '&#10003;' : '3' }}
            </div>
            <span class="text-[9px] text-white/70 mt-1">完成</span>
          </div>
        </div>
      </div>

      <!-- 商品信息 -->
      <div class="bg-white rounded-2xl p-4 shadow-sm">
        <h2 class="text-sm font-semibold text-gray-800 mb-3">商品信息</h2>
        <div v-for="(item, i) in o.items" :key="i">
          <div v-if="i > 0" class="my-3 border-t border-gray-50"></div>
          <div class="flex gap-3">
            <img :src="item.productImage || `https://picsum.photos/seed/${item.productId}/200/200`"
              class="w-16 h-16 bg-gray-100 rounded-xl object-cover shrink-0" />
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-gray-800 line-clamp-2">{{ item.productName }}</p>
              <p v-if="item.skuName" class="text-xs text-gray-400 mt-0.5">{{ item.skuName }}</p>
              <div class="flex items-center justify-between mt-2">
                <span class="text-xs text-gray-400">×{{ item.quantity }}</span>
                <span class="text-sm font-bold text-orange-500">¥{{ ((item.price * item.quantity) / 100).toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 收货信息 + 订单信息 两栏 -->
      <div class="grid grid-cols-2 gap-3">
        <div class="bg-white rounded-2xl p-4 shadow-sm">
          <h2 class="text-sm font-semibold text-gray-800 mb-3">收货信息</h2>
          <div class="space-y-2 text-xs">
            <div>
              <p class="text-gray-400">收货人</p>
              <p class="text-gray-800 font-medium mt-0.5">{{ o.receiverName }}</p>
            </div>
            <div>
              <p class="text-gray-400">手机号</p>
              <p class="text-gray-800 mt-0.5">{{ o.receiverPhone }}</p>
            </div>
            <div>
              <p class="text-gray-400">地址</p>
              <p class="text-gray-800 mt-0.5 leading-relaxed">{{ o.receiverAddr }}</p>
            </div>
          </div>
        </div>
        <div class="bg-white rounded-2xl p-4 shadow-sm">
          <h2 class="text-sm font-semibold text-gray-800 mb-3">订单信息</h2>
          <div class="space-y-2 text-xs">
            <div>
              <p class="text-gray-400">编号</p>
              <p class="text-gray-600 font-mono mt-0.5 truncate">{{ o.orderNo }}</p>
            </div>
            <div>
              <p class="text-gray-400">时间</p>
              <p class="text-gray-600 mt-0.5">{{ o.createTime }}</p>
            </div>
            <div v-if="o.discountAmount">
              <p class="text-gray-400">优惠</p>
              <p class="text-red-500 mt-0.5">-¥{{ ((o.discountAmount || 0) / 100).toFixed(2) }}</p>
            </div>
            <div>
              <p class="text-gray-400">实付</p>
              <p class="text-orange-500 font-bold text-base mt-0.5">¥{{ ((o.payAmount || o.totalAmount || 0) / 100).toFixed(2) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 - 底部固定 -->
      <div v-if="o.status === 0 || o.status === 1 || o.status === 2" class="fixed bottom-14 inset-x-0 px-4 z-40">
        <div class="flex gap-3">
          <button v-if="o.status === 0" @click="goPay"
            class="flex-1 py-3 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl font-medium active:scale-[0.98] transition-transform shadow-lg">
            去支付
          </button>
          <button v-if="o.status === 0" @click="cancelOrder"
            class="flex-1 py-3 bg-white border-2 border-red-200 text-red-500 rounded-xl font-medium active:scale-[0.98] transition-transform shadow-lg">
            取消订单
          </button>
          <button v-if="o.status === 1 || o.status === 2" @click="refundOrder"
            :disabled="refunding"
            class="flex-1 py-3 bg-white border-2 border-orange-300 text-orange-500 rounded-xl font-medium active:scale-[0.98] transition-transform shadow-lg disabled:opacity-50">
            {{ refunding ? '处理中...' : '申请退款' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== PC 端布局 ========== -->
    <div class="hidden md:block">
      <!-- 状态卡片 -->
      <div class="bg-gradient-to-br from-orange-500 to-amber-500 rounded-2xl p-6 mb-5 text-white shadow-xl shadow-orange-200">
        <div class="flex items-center gap-4">
          <div class="w-14 h-14 bg-white/20 rounded-2xl flex items-center justify-center text-2xl shrink-0 backdrop-blur">
            {{ statusIcon }}
          </div>
          <div>
            <p class="text-white/80 text-sm">订单状态</p>
            <p class="text-xl font-bold mt-1">{{ statusText }}</p>
          </div>
        </div>
        <div class="flex items-center mt-6 gap-0">
          <div class="flex flex-col items-center flex-1">
            <div class="w-7 h-7 rounded-full bg-white text-orange-500 text-xs flex items-center justify-center font-bold shadow-md">&#10003;</div>
            <span class="text-[10px] text-white/80 mt-1.5">已下单</span>
          </div>
          <div class="h-1 flex-1 mx-1" :class="progress >= 2 ? 'bg-white' : 'bg-white/30'"></div>
          <div class="flex flex-col items-center flex-1">
            <div class="w-7 h-7 rounded-full text-xs flex items-center justify-center font-bold shadow-md transition-all duration-500"
              :class="progress >= 2 ? 'bg-white text-orange-500' : 'bg-white/30 text-white/60'">
              {{ progress >= 2 ? '&#10003;' : '2' }}</div>
            <span class="text-[10px] text-white/80 mt-1.5">已发货</span>
          </div>
          <div class="h-1 flex-1 mx-1" :class="progress >= 3 ? 'bg-white' : 'bg-white/30'"></div>
          <div class="flex flex-col items-center flex-1">
            <div class="w-7 h-7 rounded-full text-xs flex items-center justify-center font-bold shadow-md transition-all duration-500"
              :class="progress >= 3 ? 'bg-white text-orange-500' : 'bg-white/30 text-white/60'">
              {{ progress >= 3 ? '&#10003;' : '3' }}</div>
            <span class="text-[10px] text-white/80 mt-1.5">已完成</span>
          </div>
        </div>
      </div>

      <!-- 两栏布局 -->
      <div class="grid grid-cols-2 gap-5 mb-4">
        <!-- 商品信息 -->
        <div class="bg-white rounded-2xl p-5 shadow-md">
          <h2 class="text-sm font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <span class="w-1 h-4 bg-orange-500 rounded-full"></span>
            商品信息
          </h2>
          <div v-for="(item, i) in o.items" :key="i" class="flex gap-4" :class="{ 'mt-4 pt-4 border-t border-gray-100': i > 0 }">
            <img :src="item.productImage || `https://picsum.photos/seed/${item.productId}/200/200`"
              class="w-20 h-20 bg-gray-100 rounded-xl object-cover shadow-sm" />
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-gray-800 truncate">{{ item.productName }}</p>
              <p v-if="item.skuName" class="text-xs text-gray-400 mt-0.5">{{ item.skuName }}</p>
              <p class="text-xs text-gray-400 mt-1">数量：{{ item.quantity }}</p>
              <div class="flex items-center justify-between mt-3">
                <span class="text-sm text-gray-400">单价 ¥{{ ((item.price || 0) / 100).toFixed(2) }}</span>
                <span class="text-sm font-bold text-orange-500">小计 ¥{{ ((item.price * item.quantity) / 100).toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：收货信息 + 订单信息 -->
        <div class="space-y-5">
          <div class="bg-white rounded-2xl p-5 shadow-md">
            <h2 class="text-sm font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <span class="w-1 h-4 bg-blue-500 rounded-full"></span>
              收货信息
            </h2>
            <div class="space-y-3">
              <div class="flex items-start gap-3">
                <span class="w-8 h-8 bg-gray-100 rounded-lg flex items-center justify-center text-sm shrink-0">&#128100;</span>
                <div>
                  <p class="text-xs text-gray-400">收货人</p>
                  <p class="text-sm text-gray-800 font-medium">{{ o.receiverName }}</p>
                </div>
              </div>
              <div class="flex items-start gap-3">
                <span class="w-8 h-8 bg-gray-100 rounded-lg flex items-center justify-center text-sm shrink-0">&#128222;</span>
                <div>
                  <p class="text-xs text-gray-400">手机号</p>
                  <p class="text-sm text-gray-800">{{ o.receiverPhone }}</p>
                </div>
              </div>
              <div class="flex items-start gap-3">
                <span class="w-8 h-8 bg-gray-100 rounded-lg flex items-center justify-center text-sm shrink-0">&#128205;</span>
                <div>
                  <p class="text-xs text-gray-400">收货地址</p>
                  <p class="text-sm text-gray-800">{{ o.receiverAddr }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="bg-white rounded-2xl p-5 shadow-md">
            <h2 class="text-sm font-semibold text-gray-800 mb-4 flex items-center gap-2">
              <span class="w-1 h-4 bg-green-500 rounded-full"></span>
              订单信息
            </h2>
            <div class="space-y-3 text-sm">
              <div class="flex justify-between py-2 border-b border-gray-100">
                <span class="text-gray-400">订单编号</span>
                <span class="font-mono text-gray-600">{{ o.orderNo }}</span>
              </div>
              <div class="flex justify-between py-2 border-b border-gray-100">
                <span class="text-gray-400">下单时间</span>
                <span class="text-gray-600">{{ o.createTime }}</span>
              </div>
              <div v-if="o.discountAmount" class="flex justify-between py-2 border-b border-gray-100">
                <span class="text-gray-400">优惠金额</span>
                <span class="text-red-500">-¥{{ ((o.discountAmount || 0) / 100).toFixed(2) }}</span>
              </div>
              <div class="flex justify-between py-2">
                <span class="text-gray-400">实付金额</span>
                <span class="text-orange-500 font-bold text-lg">¥{{ ((o.payAmount || o.totalAmount || 0) / 100).toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- PC 操作按钮 -->
      <div v-if="o.status === 0 || o.status === 1 || o.status === 2" class="flex gap-3 mb-4">
        <button v-if="o.status === 0" @click="goPay"
          class="px-8 py-3 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl font-medium hover:shadow-lg hover:shadow-orange-200 transition-all">
          去支付
        </button>
        <button v-if="o.status === 0" @click="cancelOrder"
          class="px-8 py-3 border-2 border-gray-200 text-gray-500 rounded-xl font-medium hover:border-red-300 hover:text-red-500 transition-all">
          取消订单
        </button>
        <button v-if="o.status === 1 || o.status === 2" @click="refundOrder"
          :disabled="refunding"
          class="px-8 py-3 border-2 border-orange-300 text-orange-500 rounded-xl font-medium hover:bg-orange-50 hover:border-orange-400 transition-all disabled:opacity-50">
          {{ refunding ? '处理中...' : '申请退款' }}
        </button>
      </div>
    </div>
  </div>

  <!-- 空状态 -->
  <div v-else class="text-center py-24">
    <div class="w-20 h-20 mx-auto mb-6 bg-gray-100 rounded-full flex items-center justify-center text-4xl">
      &#128533;
    </div>
    <p class="text-gray-500 font-medium">订单不存在</p>
    <router-link to="/user" class="inline-block mt-4 px-6 py-2 bg-orange-500 text-white rounded-full text-sm">
      返回个人中心
    </router-link>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useOrderStore } from '@/stores/order'
import { paymentAPI } from '@/api'

const refunding = ref(false)

const route = useRoute()
const router = useRouter()
const order = useOrderStore()

const o = computed(() => order.currentDetail)
const syncing = ref(false)

const STATUS_TEXT = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款' }
const statusText = computed(() => STATUS_TEXT[o.value?.status] || '未知')
const STATUS_ICON = { 0: '💳', 1: '💳', 2: '🚚', 3: '✅', 4: '❌', 5: '🔄', 6: '↩️' }
const statusIcon = computed(() => STATUS_ICON[o.value?.status] || '❓')

const progress = computed(() => {
  if (!o.value) return 0
  switch (o.value.status) {
    case 0: case 1: return 1
    case 2: return 2
    case 3: return 3
    case 4: case 5: case 6: return 0
    default: return 1
  }
})

function goPay() {
  router.push(`/pay?orderNo=${o.value.orderNo}`)
}

async function cancelOrder() {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '取消订单', {
      confirmButtonText: '确定',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch { return }
  try {
    await order.cancel(o.value.orderNo)
    await order.fetchDetail(o.value.orderNo)
    ElMessage.success('订单已取消')
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}

async function refundOrder() {
  let reason = ''
  try {
    const amount = (o.value.payAmount || o.value.totalAmount || 0) / 100
    const result = await ElMessageBox.prompt(
      `退款金额 ¥${amount.toFixed(2)}，请输入退款原因（选填）：`,
      '申请退款',
      {
        confirmButtonText: '确认退款',
        cancelButtonText: '取消',
        inputPlaceholder: '如：不想要了',
        inputType: 'text',
      }
    )
    reason = result.value || ''
  } catch { return }

  try {
    refunding.value = true
    const refundAmount = (o.value.payAmount || o.value.totalAmount || 0)
    await order.refund(o.value.orderNo, refundAmount, reason)
    ElMessage.success('退款申请已提交，退款处理中')
    await order.fetchDetail(o.value.orderNo)
  } catch (e) {
    ElMessage.error(e.message || '退款失败')
  } finally {
    refunding.value = false
  }
}

let pollTimer = null

async function syncPayStatus() {
  if (!o.value || o.value.status !== 0) return
  syncing.value = true
  try {
    const res = await paymentAPI.status(o.value.orderNo)
    if (res.status !== 0) {
      await order.fetchDetail(o.value.orderNo)
      stopPoll()
    }
  } catch { /* ignore */ }
  syncing.value = false
}

function startPollIfPending() {
  if (!o.value || o.value.status !== 0) return
  pollTimer = setInterval(syncPayStatus, 5000)
}

function stopPoll() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(() => {
  order.fetchDetail(route.params.orderNo).then(() => startPollIfPending())
})

onUnmounted(() => {
  stopPoll()
})
</script>
