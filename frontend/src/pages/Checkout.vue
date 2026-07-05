<template>
  <div class="max-w-2xl mx-auto px-4 md:px-0 pb-24 md:pb-8">
    <!-- 顶部导航 -->
    <div class="flex items-center gap-3 mb-5">
      <button @click="$router.back()" class="text-gray-400 hover:text-gray-600 text-xl">&larr;</button>
      <h1 class="text-lg md:text-xl font-bold text-gray-800">确认订单</h1>
    </div>

    <!-- 空状态 -->
    <div v-if="!checkout.hasItems" class="text-center py-20">
      <div class="w-20 h-20 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center text-3xl">&#128722;</div>
      <p class="text-gray-400">没有待结算的商品</p>
      <router-link to="/" class="inline-block mt-4 text-orange-500 text-sm font-medium">去逛逛</router-link>
    </div>

    <template v-else>
      <!-- 商品列表 -->
      <div class="bg-white rounded-2xl p-4 md:p-5 shadow-md mb-4">
        <h2 class="text-sm font-semibold text-gray-800 mb-3 flex items-center gap-2">
          <span class="w-1 h-4 bg-orange-500 rounded-full"></span>
          商品信息
        </h2>
        <div v-for="(item, i) in checkout.items" :key="i">
          <div v-if="i > 0" class="my-3 border-t border-gray-50"></div>
          <div class="flex gap-3">
            <img :src="item.image || `https://picsum.photos/seed/${item.productId}/200/200`"
              class="w-16 h-16 md:w-20 md:h-20 bg-gray-100 rounded-xl object-cover shrink-0" />
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-gray-800 line-clamp-1">{{ item.name }}</p>
              <p v-if="item.skuName" class="text-xs text-gray-400 mt-0.5">{{ item.skuName }}</p>
              <div class="flex items-center justify-between mt-2">
                <span class="text-xs text-gray-400">×{{ item.quantity }}</span>
                <span class="text-sm font-bold text-orange-500">¥{{ (item.price * item.quantity / 100).toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 收货地址 -->
      <div class="bg-white rounded-2xl p-4 md:p-5 shadow-md mb-4">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-sm font-semibold text-gray-800 flex items-center gap-2">
            <span class="w-1 h-4 bg-blue-500 rounded-full"></span>
            收货地址
          </h2>
          <router-link to="/user/addresses" class="text-xs text-orange-500 hover:text-orange-600">管理地址</router-link>
        </div>
        <div v-if="loading" class="text-center py-3 text-gray-400 text-sm">加载中...</div>
        <div v-else-if="addresses.length === 0" class="text-center py-4 bg-gray-50 rounded-xl">
          <p class="text-sm text-gray-400">暂无收货地址</p>
          <router-link to="/user/addresses" class="text-xs text-orange-500 mt-1 inline-block">去添加地址</router-link>
        </div>
        <div v-else class="space-y-2">
          <label
            v-for="a in addresses"
            :key="a.id"
            :class="[
              'flex items-start gap-3 p-3 rounded-xl border-2 cursor-pointer transition-all',
              selectedAddressId === a.id ? 'border-orange-400 bg-orange-50' : 'border-gray-100 hover:border-orange-200'
            ]"
          >
            <input v-model="selectedAddressId" :value="a.id" type="radio" class="mt-0.5 text-orange-500" />
            <div>
              <div class="flex items-center gap-2">
                <span class="text-sm font-medium text-gray-800">{{ a.name }}</span>
                <span class="text-xs text-gray-400">{{ a.phone }}</span>
                <span v-if="a.isDefault" class="text-[10px] text-orange-500 bg-orange-50 px-1.5 py-0.5 rounded-full">默认</span>
              </div>
              <p class="text-xs text-gray-400 mt-0.5">{{ a.label }}</p>
            </div>
          </label>
        </div>
      </div>

      <!-- 优惠券 -->
      <div class="bg-white rounded-2xl p-4 md:p-5 shadow-md mb-4">
        <h2 class="text-sm font-semibold text-gray-800 mb-3 flex items-center gap-2">
          <span class="w-1 h-4 bg-red-500 rounded-full"></span>
          优惠券
        </h2>
        <div v-if="availableCoupons.length === 0" class="text-sm text-gray-400 py-2">暂无可用优惠券</div>
        <div v-else class="space-y-2">
          <label
            v-for="c in availableCoupons"
            :key="c.id"
            :class="[
              'flex items-center gap-3 p-3 rounded-xl border-2 cursor-pointer transition-all',
              selectedCouponId === c.id ? 'border-orange-400 bg-orange-50' : 'border-gray-100 hover:border-orange-200'
            ]"
          >
            <input v-model="selectedCouponId" :value="c.id" type="radio" class="mt-0.5 text-orange-500" @change="onCouponChange" />
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-gray-800">{{ c.name }}</p>
              <p class="text-xs text-gray-400 mt-0.5">
                满 ¥{{ (c.threshold / 100).toFixed(0) }} 减 ¥{{ (c.reduce / 100).toFixed(0) }}
                <span v-if="c.expireAt" class="ml-2">· {{ c.expireAt }} 到期</span>
              </p>
            </div>
          </label>
          <label
            :class="[
              'flex items-center gap-3 p-3 rounded-xl border-2 cursor-pointer transition-all',
              selectedCouponId === null ? 'border-gray-300 bg-gray-50' : 'border-gray-100 hover:border-gray-200'
            ]"
          >
            <input v-model="selectedCouponId" :value="null" type="radio" class="mt-0.5" @change="onCouponChange" />
            <span class="text-sm text-gray-500">不使用优惠券</span>
          </label>
        </div>
      </div>

      <!-- 备注 -->
      <div class="bg-white rounded-2xl p-4 md:p-5 shadow-md mb-4">
        <h2 class="text-sm font-semibold text-gray-800 mb-3 flex items-center gap-2">
          <span class="w-1 h-4 bg-gray-400 rounded-full"></span>
          订单备注
        </h2>
        <input
          v-model="remark"
          class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all"
          placeholder="选填：对订单的备注说明"
        />
      </div>

      <!-- 订单摘要 -->
      <div class="bg-white rounded-2xl p-4 md:p-5 shadow-md mb-4">
        <h2 class="text-sm font-semibold text-gray-800 mb-3 flex items-center gap-2">
          <span class="w-1 h-4 bg-green-500 rounded-full"></span>
          价格明细
        </h2>
        <div class="space-y-2 text-sm">
          <div class="flex justify-between">
            <span class="text-gray-400">商品合计</span>
            <span class="text-gray-700">¥{{ (checkout.total / 100).toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">商品数量</span>
            <span class="text-gray-700">{{ checkout.count }} 件</span>
          </div>
          <div v-if="previewResult" class="flex justify-between">
            <span class="text-gray-400">优惠金额</span>
            <span class="text-red-500">-¥{{ ((previewResult.discountAmount || 0) / 100).toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">运费</span>
            <span class="text-green-600">免运费</span>
          </div>
          <div class="flex justify-between font-bold pt-2 border-t border-gray-200 text-base">
            <span class="text-gray-700">实付金额</span>
            <span class="text-orange-500">¥{{ ((previewResult ? previewResult.payAmount : checkout.total) / 100).toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <!-- 固定的底部提交按钮 -->
      <div class="fixed bottom-14 md:bottom-0 inset-x-0 bg-white/80 backdrop-blur border-t border-gray-100 px-4 py-3 md:relative md:bg-transparent md:border-0 md:p-0 md:mt-4 z-40">
        <div class="max-w-2xl mx-auto flex items-center justify-between md:justify-end md:gap-3">
          <div class="md:hidden">
            <span class="text-xs text-gray-400">实付</span>
            <span class="text-lg font-bold text-orange-500 ml-1">¥{{ ((previewResult ? previewResult.payAmount : checkout.total) / 100).toFixed(2) }}</span>
          </div>
          <button
            @click="submitOrder"
            :disabled="!selectedAddressId || placing"
            class="px-8 py-3 bg-gradient-to-r from-red-500 to-orange-500 text-white rounded-xl font-medium hover:shadow-lg hover:shadow-orange-200 transition-all disabled:opacity-50 disabled:cursor-not-allowed text-sm md:text-base"
          >
            {{ placing ? '提交中...' : '提交订单' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCheckoutStore } from '@/stores/checkout'
import { useAuthStore } from '@/stores/auth'
import { useAddressStore } from '@/stores/address'
import { useCouponStore } from '@/stores/coupon'
import { useOrderStore } from '@/stores/order'

const router = useRouter()
const checkout = useCheckoutStore()
const auth = useAuthStore()
const addrStore = useAddressStore()
const couponStore = useCouponStore()
const order = useOrderStore()

const loading = ref(true)
const selectedAddressId = ref(null)
const selectedCouponId = ref(null)
const remark = ref('')
const placing = ref(false)
const previewResult = ref(null)

const addresses = computed(() => addrStore.addresses)
const availableCoupons = computed(() =>
  couponStore.myCoupons.available.filter(c => c.threshold <= checkout.total)
)

function onCouponChange() {
  if (selectedCouponId.value != null) {
    previewWithCoupon(selectedCouponId.value)
  } else {
    previewResult.value = null
  }
}

async function previewWithCoupon(couponId) {
  try {
    const data = await order.preview(checkout.items, couponId)
    if (data && data.payAmount !== undefined) {
      previewResult.value = data
    } else {
      previewResult.value = null
      ElMessage.warning('该优惠券不适用当前订单')
    }
  } catch (e) {
    previewResult.value = null
    ElMessage.warning(e.message || '优惠券验证失败')
  }
}

async function submitOrder() {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  placing.value = true
  try {
    const couponId = selectedCouponId.value || null
    const res = await order.place(checkout.items, selectedAddressId.value, couponId, remark.value)
    const orderNo = typeof res === 'number' || typeof res === 'string' ? res : res?.data
    checkout.clear()
    ElMessage.success('下单成功，即将跳转支付')
    router.push(`/pay?orderNo=${orderNo}`)
  } catch (e) {
    ElMessage.error(e.message || '下单失败，请稍后重试')
  }
  placing.value = false
}

onMounted(async () => {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }
  if (!checkout.hasItems) {
    return
  }
  loading.value = true
  await Promise.all([
    addrStore.fetchList(),
    couponStore.fetchMy(),
  ])
  if (addrStore.defaultAddress) {
    selectedAddressId.value = addrStore.defaultAddress.id
  } else if (addrStore.addresses.length > 0) {
    selectedAddressId.value = addrStore.addresses[0].id
  }
  loading.value = false
})
</script>
