<template>
  <div v-if="auth.isVendor" class="px-4 md:px-0">
    <!-- 标题 -->
    <div class="flex items-center gap-3 mb-5">
      <router-link to="/vendor" class="md:hidden text-gray-400 active:text-gray-600 text-xl">&larr;</router-link>
      <h1 class="text-lg md:text-xl font-bold text-gray-800">订单管理</h1>
      <span class="text-sm text-gray-400">({{ filtered.length }} 单)</span>
    </div>

    <!-- 状态筛选 -->
    <div class="flex gap-2 mb-5 overflow-x-auto pb-1">
      <button
        v-for="s in statusTabs"
        :key="s"
        @click="tab = s"
        :class="[
          'px-4 py-2 rounded-xl text-sm font-medium whitespace-nowrap transition-all duration-300',
          tab === s
            ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-md shadow-orange-200'
            : 'bg-white text-gray-600 shadow-sm hover:shadow-md hover:text-orange-600'
        ]"
      >
        {{ s }}
        <span v-if="s !== '全部'" class="ml-1 opacity-70">
          ({{ statusCounts[s] || 0 }})
        </span>
      </button>
    </div>

    <!-- 加载中 -->
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
      <table v-else class="w-full text-sm">
        <thead>
          <tr class="border-b text-left text-gray-400 bg-gray-50/50">
            <th class="py-4 px-5 font-medium">订单号</th>
            <th class="py-4 px-5 font-medium">商品</th>
            <th class="py-4 px-5 font-medium">买家信息</th>
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
            <td class="py-4 px-5">
              <div>
                <p class="text-gray-800">{{ o.buyerName }}</p>
                <p class="text-xs text-gray-400">{{ o.buyerPhone }}</p>
              </div>
            </td>
            <td class="py-4 px-5 text-gray-600">×{{ o.qty }}</td>
            <td class="py-4 px-5 text-orange-500 font-bold">¥{{ ((o.total || 0) / 100).toFixed(2) }}</td>
            <td class="py-4 px-5">
              <span :class="statusClass(o.status)" class="text-xs px-3 py-1 rounded-full font-medium">
                {{ o.status }}
              </span>
            </td>
            <td class="py-4 px-5 text-gray-400 text-xs">{{ o.time }}</td>
            <td class="py-4 px-5">
              <div class="flex items-center gap-2">
                <router-link
                  :to="`/vendor/order/${o.orderNo}`"
                  class="text-blue-600 hover:text-blue-700 text-xs font-medium hover:bg-blue-50 px-2 py-1 rounded-lg transition-colors"
                >
                  详情
                </router-link>
                <button
                  v-if="o.status === '已支付'"
                  @click="shipOrder(o)"
                  class="text-orange-600 hover:text-orange-700 text-xs font-medium hover:bg-orange-50 px-2 py-1 rounded-lg transition-colors"
                >
                  发货
                </button>
                <button
                  v-if="o.status === '已发货'"
                  @click="completeOrder(o)"
                  class="text-green-600 hover:text-green-700 text-xs font-medium hover:bg-green-50 px-2 py-1 rounded-lg transition-colors"
                >
                  完成
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
        class="bg-white rounded-2xl p-4 shadow-md"
      >
        <div class="flex items-center justify-between mb-3">
          <span class="font-mono text-xs text-gray-400">{{ o.orderNo || '#' + o.id }}</span>
          <span :class="statusClass(o.status)" class="text-xs px-2.5 py-1 rounded-full font-medium">
            {{ o.status }}
          </span>
        </div>
        <p class="text-sm font-medium text-gray-800">{{ o.productName }}</p>
        <div class="flex items-center justify-between mt-3 text-xs text-gray-400">
          <span>{{ o.buyerName }} · {{ o.buyerPhone }}</span>
          <span>×{{ o.qty }}</span>
        </div>
        <div class="flex items-center justify-between mt-3 pt-3 border-t border-gray-100">
          <div>
            <span class="text-lg font-bold text-orange-500">¥{{ ((o.total || 0) / 100).toFixed(2) }}</span>
            <span class="text-xs text-gray-400 ml-2">{{ o.time }}</span>
          </div>
          <div class="flex gap-2">
            <router-link
              :to="`/vendor/order/${o.orderNo}`"
              class="text-blue-600 text-xs font-medium"
            >
              详情
            </router-link>
            <button
              v-if="o.status === '已支付'"
              @click="shipOrder(o)"
              class="text-orange-600 text-xs font-medium"
            >
              发货
            </button>
            <button
              v-if="o.status === '已发货'"
              @click="completeOrder(o)"
              class="text-green-600 text-xs font-medium"
            >
              完成
            </button>
          </div>
        </div>
      </div>
    </div>
  </template>
  </div>

  <!-- 非商家提示 -->
  <div v-else class="text-center py-24">
    <p class="text-gray-400">请使用商家账号<a href="/login" class="text-orange-500">登录</a></p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useOrderStore } from '@/stores/order'

const auth = useAuthStore()
const order = useOrderStore()
const tab = ref('全部')

const statusTabs = ['全部', '待支付', '已支付', '已发货', '已完成', '已取消', '退款中', '已退款']

const filtered = computed(() => {
  const list = order.shopOrders
  return tab.value === '全部' ? list : list.filter(o => o.status === tab.value)
})

const statusCounts = computed(() => {
  const counts = {}
  statusTabs.filter(s => s !== '全部').forEach(s => {
    counts[s] = order.shopOrders.filter(o => o.status === s).length
  })
  return counts
})

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

async function shipOrder(o) {
  try {
    await order.updateStatus(o.orderNo || o.id, 2)
    ElMessage.success('已发货')
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function completeOrder(o) {
  try {
    await order.updateStatus(o.orderNo || o.id, 3)
    ElMessage.success('订单已完成')
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => {
  order.fetchShopOrders()
})
</script>
