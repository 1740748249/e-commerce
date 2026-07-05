<template>
  <div class="max-w-2xl mx-auto px-4 md:px-0">
    <div class="flex items-center gap-3 mb-5">
      <button @click="$router.back()" class="md:hidden text-gray-400 active:text-gray-800 text-xl">&larr;</button>
      <h1 class="text-xl font-bold md:flex items-center gap-2">
        <span class="text-2xl">&#128722;</span> 购物车
      </h1>
      <span v-if="cart.count" class="text-sm text-gray-400">({{ cart.count }} 件商品)</span>
    </div>

    <!-- 加载中 -->
    <div v-if="cart.loading" class="text-center py-20">
      <el-icon class="is-loading text-3xl text-orange-400"><span>&#8635;</span></el-icon>
      <p class="text-gray-400 text-sm mt-3">加载中...</p>
    </div>

    <!-- 空购物车 -->
    <div v-else-if="cart.items.length === 0" class="text-center py-20">
      <div class="w-24 h-24 mx-auto mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center">
        <span class="text-4xl">&#128722;</span>
      </div>
      <p class="text-gray-500 font-medium text-lg">购物车是空的</p>
      <p class="text-sm text-gray-400 mt-1">快去挑选心仪的商品吧</p>
      <router-link to="/" class="inline-block mt-6 px-8 py-3 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl font-medium hover:shadow-lg hover:shadow-orange-200 transition-all duration-300">
        去逛逛
      </router-link>
    </div>

    <!-- 购物车列表 -->
    <div v-else class="space-y-4 mb-24 md:mb-0">
      <div
        v-for="item in cart.items"
        :key="item.id"
        class="bg-white rounded-2xl p-4 md:p-5 shadow-md hover:shadow-lg transition-shadow flex items-center gap-4"
      >
        <img :src="item.image" class="w-20 h-20 md:w-24 md:h-24 object-cover rounded-xl shrink-0 shadow-sm" />
        <div class="flex-1 min-w-0">
          <h3 class="text-sm font-medium text-gray-800 truncate">{{ item.name }}</h3>
          <p v-if="item.skuName" class="text-xs text-gray-400 mt-0.5">{{ item.skuName }}</p>
          <p class="text-xs text-gray-400 mt-1 flex items-center gap-1">
            <span class="w-1.5 h-1.5 bg-green-400 rounded-full"></span>
            {{ item.shopName }}
          </p>
          <div class="flex items-baseline gap-0.5 mt-2">
            <span class="text-xs text-orange-500">¥</span>
            <span class="text-lg font-bold text-orange-500">{{ (item.price / 100).toFixed(2) }}</span>
          </div>
        </div>
        <div class="flex flex-col items-end gap-3">
          <button @click="cart.remove(item.id)" class="text-gray-300 hover:text-red-400 text-sm hover:bg-red-50 p-1 rounded-lg transition-colors">&#10005;</button>
          <div class="flex items-center border-2 border-gray-200 rounded-xl overflow-hidden">
            <button @click="cart.updateQty(item.id, item.qty - 1)" class="px-3 py-2 text-gray-500 hover:bg-gray-100 active:bg-gray-200 transition-colors">−</button>
            <span class="px-4 py-2 text-sm font-medium bg-gray-50 min-w-[40px] text-center">{{ item.qty }}</span>
            <button @click="cart.updateQty(item.id, item.qty + 1)" class="px-3 py-2 text-gray-500 hover:bg-gray-100 active:bg-gray-200 transition-colors">+</button>
          </div>
          <div class="text-sm font-bold text-orange-500">¥{{ (item.price * item.qty / 100).toFixed(2) }}</div>
        </div>
      </div>

      <!-- PC 底部结算栏 -->
      <div class="hidden md:flex bg-white rounded-2xl p-5 shadow-md items-center justify-between">
        <div class="flex items-center gap-4">
          <span class="text-sm text-gray-500">共 <span class="font-bold text-gray-800 text-base">{{ cart.count }}</span> 件商品</span>
          <div class="h-4 w-px bg-gray-200"></div>
          <button @click="cart.clear()" class="text-sm text-gray-400 hover:text-red-500 transition-colors">清空购物车</button>
        </div>
        <div class="flex items-center gap-6">
          <div class="text-right">
            <span class="text-sm text-gray-500">合计：</span>
            <span class="text-2xl font-bold text-orange-500">¥{{ (cart.total / 100).toFixed(2) }}</span>
          </div>
          <button @click="openCheckout" class="px-12 py-3 bg-gradient-to-r from-red-500 to-orange-500 text-white rounded-xl hover:shadow-lg hover:shadow-orange-200 font-semibold transition-all duration-300 hover:-translate-y-0.5">
            提交订单
          </button>
        </div>
      </div>
    </div>

    <!-- 移动端底部结算栏 -->
    <div v-if="cart.items.length > 0" class="md:hidden fixed bottom-14 inset-x-0 glass border-t border-white/20 px-4 py-3 flex items-center justify-between z-40">
      <div>
        <span class="text-xs text-gray-400">共 {{ cart.count }} 件</span>
        <div class="flex items-baseline gap-0.5 mt-0.5">
          <span class="text-sm text-orange-500">¥</span>
          <span class="text-xl font-bold text-orange-500">{{ (cart.total / 100).toFixed(2) }}</span>
        </div>
      </div>
      <button @click="openCheckout" class="px-10 py-3 bg-gradient-to-r from-red-500 to-orange-500 text-white rounded-xl font-medium active:scale-[0.98] transition-transform">
        结算
      </button>
    </div>

  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useCheckoutStore } from '@/stores/checkout'

const router = useRouter()
const cart = useCartStore()
const auth = useAuthStore()
const checkout = useCheckoutStore()

function openCheckout() {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }
  checkout.setCartItems(cart.items)
  router.push('/checkout')
}

onMounted(() => {
  if (auth.isLoggedIn) cart.fetchCart()
})
</script>
