<template>
  <div v-if="auth.isLoggedIn" class="max-w-5xl mx-auto px-4 md:px-0">
    <!-- 用户信息卡片 -->
    <div class="bg-gradient-to-br from-orange-500 to-amber-500 rounded-2xl p-5 md:p-6 mb-5 shadow-xl shadow-orange-200 text-white">
      <div class="flex items-center gap-4">
        <div class="w-16 h-16 md:w-20 md:h-20 bg-white/20 backdrop-blur rounded-2xl flex items-center justify-center text-2xl md:text-3xl font-bold shadow-lg shrink-0">
          {{ auth.user?.name?.[0] || 'U' }}
        </div>
        <div class="flex-1 min-w-0">
          <p class="font-bold text-lg md:text-xl">{{ auth.user?.name }}</p>
          <p class="text-white/80 text-sm mt-1">{{ auth.user?.phone }}</p>
          <p class="text-white/60 text-xs mt-0.5 truncate">{{ auth.user?.addr }}</p>
        </div>
      </div>
      <div class="flex items-center gap-2 mt-4">
        <router-link
          to="/user/profile"
          class="flex-1 text-center bg-white/20 backdrop-blur text-white border border-white/30 rounded-xl px-4 py-2 text-xs font-medium hover:bg-white/30 transition-colors"
        >
          编辑资料
        </router-link>
        <button
          @click="handleLogout"
          class="flex-1 text-center bg-white/20 backdrop-blur text-white border border-white/30 rounded-xl px-4 py-2 text-xs font-medium hover:bg-red-400/50 transition-colors"
        >
          退出账号
        </button>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="grid grid-cols-2 gap-3 mb-5">
      <router-link
        to="/user/addresses"
        class="bg-white rounded-2xl p-4 shadow-md flex items-center gap-3 active:scale-[0.98] transition-transform"
      >
        <div class="w-10 h-10 bg-orange-100 rounded-xl flex items-center justify-center text-xl">📍</div>
        <div>
          <p class="text-sm font-medium text-gray-800">收货地址</p>
          <p class="text-xs text-gray-400 mt-0.5">管理收货地址</p>
        </div>
      </router-link>
      <router-link
        to="/user/profile"
        class="bg-white rounded-2xl p-4 shadow-md flex items-center gap-3 active:scale-[0.98] transition-transform"
      >
        <div class="w-10 h-10 bg-blue-100 rounded-xl flex items-center justify-center text-xl">👤</div>
        <div>
          <p class="text-sm font-medium text-gray-800">个人信息</p>
          <p class="text-xs text-gray-400 mt-0.5">编辑个人资料</p>
        </div>
      </router-link>
    </div>

    <!-- 开店入口（非商家用户） -->
    <div v-if="!auth.isVendor" class="bg-white rounded-2xl p-4 mb-5 shadow-md">
      <div class="flex items-center gap-3">
        <span class="text-2xl">🏪</span>
        <div class="flex-1">
          <p class="text-sm font-medium text-gray-800">成为商家</p>
          <p class="text-xs text-gray-400 mt-0.5">申请开店，开始售卖您的商品</p>
        </div>
        <router-link
          v-if="!auth.shop"
          to="/user/shop/apply"
          class="bg-gradient-to-r from-orange-500 to-amber-500 text-white px-4 py-2 rounded-xl text-xs font-medium shadow-md shadow-orange-100"
        >
          申请开店
        </router-link>
        <span v-else-if="auth.shop.approved === 0" class="text-xs text-gray-400 bg-gray-50 px-3 py-1.5 rounded-lg">
          ⏳ 审批中
        </span>
        <router-link
          v-else-if="auth.shop.approved === 2"
          to="/user/shop/apply"
          class="bg-red-500 text-white px-4 py-2 rounded-xl text-xs font-medium"
        >
          重新申请
        </router-link>
      </div>
    </div>

    <!-- 订单统计条 -->
    <div class="grid grid-cols-4 md:grid-cols-7 gap-2 md:gap-3 mb-5">
      <div
        @click="tab = ''"
        :class="[
          'bg-white rounded-2xl p-3 md:p-4 text-center shadow-md cursor-pointer transition-all duration-300',
          tab === '' ? 'ring-2 ring-orange-400 shadow-orange-100' : 'hover:shadow-lg'
        ]"
      >
        <div class="text-lg md:text-2xl font-bold text-gray-800">{{ orders.length }}</div>
        <div class="text-[10px] md:text-xs text-gray-400 mt-0.5">全部</div>
      </div>
      <div
        @click="tab = '待支付'"
        :class="[
          'bg-white rounded-2xl p-2 md:p-4 text-center shadow-md cursor-pointer transition-all duration-300',
          tab === '待支付' ? 'ring-2 ring-red-400 shadow-red-100' : 'hover:shadow-lg'
        ]"
      >
        <div class="text-lg md:text-2xl font-bold text-red-600">{{ orders.filter(o => o.status === '待支付').length }}</div>
        <div class="text-[10px] md:text-xs text-gray-400 mt-0.5">待支付</div>
      </div>
      <div
        @click="tab = '已支付'"
        :class="[
          'bg-white rounded-2xl p-2 md:p-4 text-center shadow-md cursor-pointer transition-all duration-300',
          tab === '已支付' ? 'ring-2 ring-yellow-400 shadow-yellow-100' : 'hover:shadow-lg'
        ]"
      >
        <div class="text-lg md:text-2xl font-bold text-yellow-600">{{ orders.filter(o => o.status === '已支付').length }}</div>
        <div class="text-[10px] md:text-xs text-gray-400 mt-0.5">已支付</div>
      </div>
      <div
        @click="tab = '已发货'"
        :class="[
          'bg-white rounded-2xl p-2 md:p-4 text-center shadow-md cursor-pointer transition-all duration-300',
          tab === '已发货' ? 'ring-2 ring-blue-400 shadow-blue-100' : 'hover:shadow-lg'
        ]"
      >
        <div class="text-lg md:text-2xl font-bold text-blue-600">{{ orders.filter(o => o.status === '已发货').length }}</div>
        <div class="text-[10px] md:text-xs text-gray-400 mt-0.5">已发货</div>
      </div>
      <div
        @click="tab = '已完成'"
        :class="[
          'bg-white rounded-2xl p-2 md:p-4 text-center shadow-md cursor-pointer transition-all duration-300',
          tab === '已完成' ? 'ring-2 ring-green-400 shadow-green-100' : 'hover:shadow-lg'
        ]"
      >
        <div class="text-lg md:text-2xl font-bold text-green-600">{{ orders.filter(o => o.status === '已完成').length }}</div>
        <div class="text-[10px] md:text-xs text-gray-400 mt-0.5">已完成</div>
      </div>
      <div
        @click="tab = '退款中'"
        :class="[
          'bg-white rounded-2xl p-2 md:p-4 text-center shadow-md cursor-pointer transition-all duration-300',
          tab === '退款中' ? 'ring-2 ring-purple-400 shadow-purple-100' : 'hover:shadow-lg'
        ]"
      >
        <div class="text-lg md:text-2xl font-bold text-purple-600">{{ orders.filter(o => o.status === '退款中').length }}</div>
        <div class="text-[10px] md:text-xs text-gray-400 mt-0.5">退款中</div>
      </div>
      <div
        @click="tab = '已退款'"
        :class="[
          'bg-white rounded-2xl p-2 md:p-4 text-center shadow-md cursor-pointer transition-all duration-300',
          tab === '已退款' ? 'ring-2 ring-slate-400 shadow-slate-100' : 'hover:shadow-lg'
        ]"
      >
        <div class="text-lg md:text-2xl font-bold text-slate-600">{{ orders.filter(o => o.status === '已退款').length }}</div>
        <div class="text-[10px] md:text-xs text-gray-400 mt-0.5">已退款</div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="order.loading" class="text-center py-12 text-gray-400">
      <el-icon class="is-loading text-2xl text-orange-400"><span>&#8635;</span></el-icon>
      <p class="text-sm mt-2">加载中...</p>
    </div>

    <template v-else>
    <!-- PC 端表格 -->
    <div class="hidden md:block bg-white rounded-2xl shadow-md overflow-hidden">
      <div v-if="filtered.length === 0" class="text-center py-16">
        <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center text-3xl">
          &#128196;
        </div>
        <p class="text-gray-400">暂无订单</p>
      </div>
      <table v-else class="w-full table-fixed text-sm">
        <thead>
          <tr class="border-b text-left text-gray-400 bg-gray-50/50">
            <th class="py-4 px-5 font-medium">订单号</th>
            <th class="py-4 px-5 font-medium">商品</th>
            <th class="py-4 px-5 font-medium">数量</th>
            <th class="py-4 px-5 font-medium">金额</th>
            <th class="py-4 px-5 font-medium">状态</th>
            <th class="py-4 px-5 font-medium">时间</th>
            <th class="py-4 px-5 font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="o in filtered"
            :key="o.id"
            class="border-b last:border-0 hover:bg-gray-50/50 transition-colors"
          >
            <td class="py-4 px-5 font-mono text-xs text-gray-500">{{ o.orderNo || '#' + o.id }}</td>
            <td class="py-4 px-5 font-medium text-gray-800">{{ o.productName }}</td>
            <td class="py-4 px-5 text-gray-600">×{{ o.qty }}</td>
            <td class="py-4 px-5 text-orange-500 font-bold">¥{{ (o.total / 100).toFixed(2) }}</td>
            <td class="py-4 px-5">
              <span :class="statusClass(o.status)" class="text-xs px-3 py-1 rounded-full font-medium">
                {{ o.status }}
              </span>
            </td>
            <td class="py-4 px-5 text-gray-400 text-xs">{{ o.time }}</td>
            <td class="py-4 px-5">
              <div class="flex items-center gap-2">
                <router-link
                  :to="`/user/order/${o.orderNo}`"
                  class="text-orange-500 text-xs font-medium hover:text-orange-600 hover:underline"
                >
                  查看详情
                </router-link>
                <button
                  v-if="o.status === '待支付'"
                  @click="goPay(o)"
                  class="text-orange-600 hover:text-orange-700 text-xs font-medium hover:underline transition-colors"
                >
                  去支付
                </button>
                <button
                  v-if="o.status === '待支付'"
                  @click="cancelOrder(o)"
                  class="text-gray-400 hover:text-red-500 text-xs font-medium hover:underline transition-colors"
                >
                  取消
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 移动端卡片 -->
    <div class="md:hidden space-y-3">
      <div v-if="filtered.length === 0" class="text-center py-16 bg-white rounded-2xl">
        <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center text-3xl">
          &#128196;
        </div>
        <p class="text-gray-400">暂无订单</p>
      </div>
      <div
        v-for="o in filtered"
        :key="o.id"
        @click="$router.push(`/user/order/${o.orderNo}`)"
        class="bg-white rounded-2xl p-4 shadow-md active:shadow-lg cursor-pointer transition-shadow hover:ring-2 hover:ring-orange-200"
      >
        <div class="flex items-center justify-between mb-2">
          <span class="font-mono text-xs text-gray-400">{{ o.orderNo || '#' + o.id }}</span>
          <span :class="statusClass(o.status)" class="text-xs px-2.5 py-1 rounded-full font-medium">
            {{ o.status }}
          </span>
        </div>
        <p class="text-sm font-medium text-gray-800">{{ o.productName }}</p>
        <div class="flex items-center justify-between mt-3">
          <span class="text-xs text-gray-400">×{{ o.qty }} · {{ o.time }}</span>
          <span class="text-base font-bold text-orange-500">¥{{ (o.total / 100).toFixed(2) }}</span>
        </div>
      </div>
    </div>
  </template>
  </div>

  <!-- 未登录状态 -->
  <div v-else class="text-center py-24">
    <div class="w-24 h-24 mx-auto mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center">
      <span class="text-4xl">&#128100;</span>
    </div>
    <p class="text-gray-500 font-medium text-lg">登录后查看个人中心</p>
    <p class="text-sm text-gray-400 mt-1">管理您的订单和个人信息</p>
    <router-link
      to="/login"
      class="inline-block mt-6 px-10 py-3 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl font-medium hover:shadow-lg hover:shadow-orange-200 transition-all duration-300"
    >
      立即登录
    </router-link>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useOrderStore } from '@/stores/order'

const router = useRouter()
const auth = useAuthStore()
const order = useOrderStore()
const tab = ref('')

const orders = computed(() => order.userOrders)
const filtered = computed(() => tab.value ? orders.value.filter(o => o.status === tab.value) : orders.value)

const statusClass = (s) => {
  switch (s) {
    case '待支付': return 'bg-red-100 text-red-700'
    case '已支付': return 'bg-yellow-100 text-yellow-700'
    case '已发货': return 'bg-blue-100 text-blue-700'
    case '已完成': return 'bg-green-100 text-green-700'
    case '已取消': return 'bg-gray-100 text-gray-500'
    case '退款中': return 'bg-purple-100 text-purple-700'
    case '已退款': return 'bg-slate-100 text-slate-600'
    default: return 'bg-gray-100 text-gray-600'
  }
}

function goPay(o) {
  router.push(`/pay?orderNo=${o.orderNo}`)
}

async function cancelOrder(o) {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '取消订单', {
      confirmButtonText: '确定',
      cancelButtonText: '返回',
      type: 'warning',
    })
  } catch { return }
  try {
    await order.cancel(o.orderNo || o.id)
    ElMessage.success('订单已取消')
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}

function handleLogout() {
  auth.logout()
  router.push('/')
}

onMounted(() => {
  order.fetchMyOrders()
})
</script>
