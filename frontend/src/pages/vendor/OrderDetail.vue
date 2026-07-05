<template>
  <div v-if="o" class="px-4 md:px-0">
    <!-- 返回按钮 -->
    <div class="flex items-center gap-3 mb-5">
      <button
        @click="$router.push('/vendor/orders')"
        class="text-gray-400 hover:text-gray-600 flex items-center gap-1 transition-colors"
      >
        &larr; 返回
      </button>
      <h1 class="text-lg md:text-xl font-bold text-gray-800">订单详情</h1>
      <span class="font-mono text-sm text-gray-400">{{ o.orderNo || '#' + o.id }}</span>
    </div>

    <!-- PC 端布局 -->
    <div class="hidden md:grid grid-cols-2 gap-5">
      <!-- 商品信息卡片 -->
      <div class="bg-white rounded-2xl p-6 shadow-md">
        <h2 class="text-base font-semibold text-gray-800 mb-5 flex items-center gap-2">
          <span class="w-1 h-5 bg-orange-500 rounded-full"></span>
          商品信息
        </h2>
        <div class="flex gap-4 mb-5">
          <img
            :src="o.productImage || `https://picsum.photos/seed/${o.id}/200/200`"
            class="w-24 h-24 bg-gray-100 rounded-xl object-cover shadow-sm"
          />
          <div class="flex-1">
            <p class="font-medium text-gray-800">{{ o.productName }}</p>
            <p v-if="o.items?.[0]?.skuName" class="text-xs text-gray-400 mt-0.5">{{ o.items[0].skuName }}</p>
            <p class="text-sm text-gray-400 mt-1">数量：{{ o.qty }}</p>
            <p class="text-orange-500 font-bold text-lg mt-2">¥{{ ((o.total || 0) / 100).toFixed(2) }}</p>
          </div>
        </div>
        <div class="space-y-3 text-sm bg-gray-50 rounded-xl p-4">
          <div class="flex justify-between">
            <span class="text-gray-400">单价</span>
            <span class="text-gray-600">¥{{ ((o.price || 0) / 100).toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">数量</span>
            <span class="text-gray-600">{{ o.qty }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">订单总额</span>
            <span class="text-orange-500 font-bold">¥{{ ((o.total || 0) / 100).toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">下单时间</span>
            <span class="text-gray-600">{{ o.time }}</span>
          </div>
          <div class="flex justify-between items-center pt-2 border-t border-gray-200">
            <span class="text-gray-400">订单状态</span>
            <span :class="statusClass(o.status)" class="text-sm px-3 py-1 rounded-full font-medium">
              {{ o.status }}
            </span>
          </div>
        </div>
        <!-- 操作按钮 -->
        <div class="flex gap-3 mt-5">
          <button
            v-if="o.status === '已支付'"
            @click="order.updateStatus(o.orderNo || o.id, 2)"
            class="flex-1 py-3 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl font-medium hover:shadow-lg hover:shadow-orange-200 transition-all"
          >
            确认发货
          </button>
          <button
            v-if="o.status === '已发货'"
            @click="order.updateStatus(o.orderNo || o.id, 3)"
            class="flex-1 py-3 bg-gradient-to-r from-green-500 to-emerald-500 text-white rounded-xl font-medium hover:shadow-lg hover:shadow-green-200 transition-all"
          >
            标记完成
          </button>
        </div>
      </div>

      <!-- 买家信息卡片 -->
      <div class="bg-white rounded-2xl p-6 shadow-md">
        <h2 class="text-base font-semibold text-gray-800 mb-5 flex items-center gap-2">
          <span class="w-1 h-5 bg-blue-500 rounded-full"></span>
          买家信息
        </h2>
        <div class="space-y-4">
          <div class="flex items-center gap-4 p-4 bg-gray-50 rounded-xl">
            <div class="w-12 h-12 bg-gradient-to-br from-blue-400 to-blue-600 rounded-xl flex items-center justify-center text-white text-xl font-bold">
              {{ (o.buyerName || '?')[0] }}
            </div>
            <div>
              <p class="font-medium text-gray-800">{{ o.buyerName }}</p>
              <p class="text-sm text-gray-400">买家</p>
            </div>
          </div>
          <div class="flex items-center gap-3 p-4 bg-gray-50 rounded-xl">
            <span class="w-10 h-10 bg-gray-200 rounded-lg flex items-center justify-center text-lg">&#128222;</span>
            <div>
              <p class="text-xs text-gray-400">手机号</p>
              <p class="text-sm text-gray-800 font-medium">{{ o.buyerPhone }}</p>
            </div>
          </div>
          <div class="flex items-start gap-3 p-4 bg-gray-50 rounded-xl">
            <span class="w-10 h-10 bg-gray-200 rounded-lg flex items-center justify-center text-lg shrink-0">&#128205;</span>
            <div>
              <p class="text-xs text-gray-400">收货地址</p>
              <p class="text-sm text-gray-800">{{ o.buyerAddr }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端布局 -->
    <div class="md:hidden space-y-4">
      <!-- 状态卡片 -->
      <div class="bg-gradient-to-br from-orange-500 to-amber-500 rounded-2xl p-5 text-white shadow-xl shadow-orange-200">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-white/80 text-sm">订单状态</p>
            <p class="text-xl font-bold mt-1">{{ o.status }}</p>
          </div>
          <div class="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center text-2xl">
            {{ statusIcon }}
          </div>
        </div>
      </div>

      <!-- 商品信息 -->
      <div class="bg-white rounded-2xl p-4 shadow-md">
        <h2 class="text-sm font-semibold text-gray-800 mb-3">商品信息</h2>
        <div class="flex gap-3">
          <img
            :src="o.productImage || `https://picsum.photos/seed/${o.id}/200/200`"
            class="w-16 h-16 bg-gray-100 rounded-xl object-cover shadow-sm"
          />
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-gray-800 truncate">{{ o.productName }}</p>
            <div class="flex items-center justify-between mt-2">
              <span class="text-xs text-gray-400">×{{ o.qty }}</span>
              <span class="text-orange-500 font-bold">¥{{ ((o.total || 0) / 100).toFixed(2) }}</span>
            </div>
          </div>
        </div>
        <!-- 操作按钮 -->
        <div class="flex gap-3 mt-4 pt-4 border-t border-gray-100">
          <button
            v-if="o.status === '已支付'"
            @click="order.updateStatus(o.orderNo || o.id, 2)"
            class="flex-1 py-2.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl text-sm font-medium"
          >
            确认发货
          </button>
          <button
            v-if="o.status === '已发货'"
            @click="order.updateStatus(o.orderNo || o.id, 3)"
            class="flex-1 py-2.5 bg-gradient-to-r from-green-500 to-emerald-500 text-white rounded-xl text-sm font-medium"
          >
            标记完成
          </button>
        </div>
      </div>

      <!-- 买家信息 -->
      <div class="bg-white rounded-2xl p-4 shadow-md">
        <h2 class="text-sm font-semibold text-gray-800 mb-3">买家信息</h2>
        <div class="space-y-3">
          <div class="flex items-center gap-3">
            <span class="text-gray-400 text-sm w-14">姓名</span>
            <span class="text-gray-800">{{ o.buyerName }}</span>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-gray-400 text-sm w-14">手机</span>
            <span class="text-gray-800">{{ o.buyerPhone }}</span>
          </div>
          <div class="flex items-start gap-3">
            <span class="text-gray-400 text-sm w-14 shrink-0">地址</span>
            <span class="text-gray-800">{{ o.buyerAddr }}</span>
          </div>
        </div>
      </div>

      <!-- 订单详情 -->
      <div class="bg-white rounded-2xl p-4 shadow-md">
        <h2 class="text-sm font-semibold text-gray-800 mb-3">订单详情</h2>
        <div class="space-y-2 text-sm">
          <div class="flex justify-between">
            <span class="text-gray-400">订单编号</span>
            <span class="font-mono text-gray-600">{{ o.orderNo || '#' + o.id }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">下单时间</span>
            <span class="text-gray-600">{{ o.time }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">单价</span>
            <span class="text-gray-600">¥{{ ((o.price || 0) / 100).toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">数量</span>
            <span class="text-gray-600">{{ o.qty }}</span>
          </div>
          <div class="flex justify-between pt-2 border-t border-gray-100">
            <span class="text-gray-400">订单总额</span>
            <span class="text-orange-500 font-bold">¥{{ ((o.total || 0) / 100).toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 空状态 -->
  <div v-else class="text-center py-24">
    <div class="w-20 h-20 mx-auto mb-6 bg-gray-100 rounded-full flex items-center justify-center text-4xl">
      &#128533;
    </div>
    <p class="text-gray-500 font-medium">订单不存在</p>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useOrderStore } from '@/stores/order'

const route = useRoute()
const order = useOrderStore()
const o = computed(() => order.shopOrders.find(o => String(o.orderNo) === String(route.params.orderNo)))

const STATUS_ICON = { '待支付': '💳', '已支付': '🚚', '已发货': '📦', '已完成': '✅', '已取消': '❌', '退款中': '🔄', '已退款': '↩️' }
const statusIcon = computed(() => STATUS_ICON[o.value?.status] || '❓')

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

onMounted(() => {
  if (order.shopOrders.length === 0) {
    order.fetchShopOrders()
  }
})
</script>
