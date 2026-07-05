<template>
  <!-- PC 顶部导航 -->
  <nav class="glass shadow-sm border-b border-white/20 sticky top-0 z-50 hidden md:block">
    <div class="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
      <!-- 左侧 Logo + 导航 -->
      <div class="flex items-center gap-10">
        <router-link to="/" class="flex items-center gap-2 group">
          <div class="w-9 h-9 bg-gradient-to-br from-orange-400 to-orange-600 rounded-xl flex items-center justify-center shadow-md shadow-orange-200 group-hover:shadow-orange-300 transition-shadow">
            <span class="text-white font-bold">多</span>
          </div>
          <span class="text-xl font-bold text-gradient">多多商城</span>
        </router-link>
        <div class="flex gap-6">
          <router-link
            to="/"
            :class="[
              'text-sm font-medium transition-all duration-300 relative py-1',
              $route.path === '/' ? 'text-orange-500' : 'text-gray-600 hover:text-orange-500'
            ]"
          >
            首页
            <span v-if="$route.path === '/'" class="absolute -bottom-1 left-0 right-0 h-0.5 bg-gradient-to-r from-orange-400 to-orange-500 rounded-full"></span>
          </router-link>
          <router-link
            v-if="isLoggedIn"
            to="/user"
            :class="[
              'text-sm font-medium transition-all duration-300 relative py-1',
              $route.path.startsWith('/user') ? 'text-orange-500' : 'text-gray-600 hover:text-orange-500'
            ]"
          >
            我的订单
            <span v-if="$route.path.startsWith('/user')" class="absolute -bottom-1 left-0 right-0 h-0.5 bg-gradient-to-r from-orange-400 to-orange-500 rounded-full"></span>
          </router-link>
          <router-link
            to="/cart"
            :class="[
              'text-sm font-medium transition-all duration-300 relative py-1',
              $route.path === '/cart' ? 'text-orange-500' : 'text-gray-600 hover:text-orange-500'
            ]"
          >
            购物车
            <span v-if="$route.path === '/cart'" class="absolute -bottom-1 left-0 right-0 h-0.5 bg-gradient-to-r from-orange-400 to-orange-500 rounded-full"></span>
          </router-link>
          <router-link
            to="/coupons"
            :class="[
              'text-sm font-medium transition-all duration-300 relative py-1 flex items-center gap-1',
              $route.path === '/coupons' ? 'text-orange-500' : 'text-gray-600 hover:text-orange-500'
            ]"
          >
            <span class="text-xs">🎫</span>
            优惠券
            <span v-if="$route.path === '/coupons'" class="absolute -bottom-1 left-0 right-0 h-0.5 bg-gradient-to-r from-orange-400 to-orange-500 rounded-full"></span>
          </router-link>
        </div>
      </div>

      <!-- 右侧用户区 -->
      <div class="flex items-center gap-5">
        <template v-if="isLoggedIn">
          <!-- 购物车 -->
          <router-link to="/cart" class="relative text-gray-500 hover:text-orange-500 transition-colors group">
            <span class="text-xl group-hover:scale-110 transition-transform inline-block">&#128722;</span>
            <span v-if="cartCount" class="absolute -top-2 -right-2 bg-gradient-to-r from-red-500 to-orange-500 text-white text-[10px] font-bold rounded-full w-5 h-5 flex items-center justify-center shadow-md animate-pulse">
              {{ cartCount }}
            </span>
          </router-link>

          <!-- 商家后台入口 -->
          <template v-if="isVendor">
            <router-link
              to="/vendor"
              class="flex items-center gap-1.5 text-sm text-blue-600 hover:text-blue-700 font-medium bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-lg transition-colors"
            >
              <span>&#127978;</span>
              商家后台
            </router-link>
          </template>

          <!-- 非商家：申请开店或状态提示 -->
          <template v-else>
            <!-- 未申请过 -->
            <router-link
              v-if="!shop"
              to="/user/shop/apply"
              class="flex items-center gap-1.5 text-sm text-orange-600 hover:text-orange-700 font-medium bg-orange-50 hover:bg-orange-100 px-3 py-1.5 rounded-lg transition-colors"
            >
              <span>&#127978;</span>
              申请开店
            </router-link>

            <!-- 待审批 -->
            <span v-else-if="shop.approved === 0" class="flex items-center gap-1.5 text-xs text-gray-400 bg-gray-50 px-3 py-1.5 rounded-lg cursor-default">
              &#9200; 店铺审批中
            </span>

            <!-- 已拒绝 -->
            <router-link
              v-else-if="shop.approved === 2"
              to="/user/shop/apply"
              class="flex items-center gap-1.5 text-sm text-red-500 hover:text-red-600 font-medium bg-red-50 hover:bg-red-100 px-3 py-1.5 rounded-lg transition-colors"
              title="您的开店申请未通过，点击重新申请"
            >
              <span>&#127978;</span>
              重新申请
            </router-link>
          </template>

          <!-- 头像下拉菜单 -->
          <div class="relative" @mouseenter="showDropdown = true" @mouseleave="showDropdown = false">
            <button class="flex items-center gap-2 text-sm text-gray-700 hover:text-orange-500 transition-colors cursor-pointer">
              <div class="w-8 h-8 bg-gradient-to-br from-orange-100 to-orange-200 rounded-full flex items-center justify-center text-orange-600 font-medium text-xs">
                {{ user.name?.[0] || 'U' }}
              </div>
              <span class="font-medium">{{ user.name }}</span>
              <span class="text-gray-300 text-xs transition-transform" :class="showDropdown && 'rotate-180'">▼</span>
            </button>

            <!-- 下拉菜单 -->
            <Transition name="dropdown">
              <div v-if="showDropdown" class="absolute right-0 mt-2 w-44 bg-white rounded-xl shadow-xl border border-gray-100 py-2 z-50">
                <router-link
                  to="/user/profile"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-orange-50 hover:text-orange-600 transition-colors"
                  @click="showDropdown = false"
                >
                  <span>&#128100;</span> 个人信息
                </router-link>
                <router-link
                  to="/user/addresses"
                  class="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 hover:bg-orange-50 hover:text-orange-600 transition-colors"
                  @click="showDropdown = false"
                >
                  <span>&#128205;</span> 收货地址
                </router-link>
                <div class="border-t border-gray-100 my-1"></div>
                <button
                  @click="handleLogout"
                  class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-500 hover:bg-red-50 transition-colors"
                >
                  <span>&#128682;</span> 退出登录
                </button>
              </div>
            </Transition>
          </div>
        </template>

        <template v-else>
          <router-link
            to="/login"
            :class="[
              'text-sm font-medium transition-all duration-300',
              $route.path === '/login'
                ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white px-5 py-2 rounded-xl shadow-lg shadow-orange-200'
                : 'text-gray-600 hover:text-orange-500'
            ]"
          >
            登录
          </router-link>
          <router-link
            to="/register"
            :class="[
              'text-sm font-medium transition-all duration-300',
              $route.path === '/register'
                ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white px-5 py-2 rounded-xl shadow-lg shadow-orange-200'
                : 'text-gray-600 hover:text-orange-500'
            ]"
          >
            注册
          </router-link>
        </template>
      </div>
    </div>
  </nav>

  <!-- 移动端顶部 -->
  <header v-if="!isProductDetail" class="md:hidden glass border-b border-white/20 sticky top-0 z-50 px-4 h-12 flex items-center justify-between">
    <router-link to="/" class="flex items-center gap-1.5">
      <div class="w-7 h-7 bg-gradient-to-br from-orange-400 to-orange-600 rounded-lg flex items-center justify-center">
        <span class="text-white text-sm font-bold">多</span>
      </div>
      <span class="text-base font-bold text-orange-500">多多商城</span>
    </router-link>
    <div class="flex items-center gap-3">
      <template v-if="isLoggedIn">
        <router-link to="/cart" class="relative">
          <span class="text-lg text-gray-600">&#128722;</span>
          <span v-if="cartCount" class="absolute -top-1.5 -right-1.5 bg-gradient-to-r from-red-500 to-orange-500 text-white text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
            {{ cartCount }}
          </span>
        </router-link>
        <router-link to="/user/profile" class="w-7 h-7 bg-orange-100 rounded-full flex items-center justify-center text-orange-600 text-xs font-medium">
          {{ user.name?.[0] || 'U' }}
        </router-link>
      </template>
      <template v-else>
        <router-link
          to="/login"
          :class="[
            'text-xs font-medium transition-all duration-300',
            $route.path === '/login'
              ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white px-3 py-1.5 rounded-lg shadow-md shadow-orange-200'
              : 'text-gray-600'
          ]"
        >登录</router-link>
        <router-link
          to="/register"
          :class="[
            'text-xs font-medium transition-all duration-300',
            $route.path === '/register'
              ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white px-3 py-1.5 rounded-lg shadow-md shadow-orange-200'
              : 'text-gray-600'
          ]"
        >注册</router-link>
      </template>
    </div>
  </header>

  <!-- 移动端底部 Tab 栏 -->
  <nav v-if="!isProductDetail" class="md:hidden fixed bottom-0 inset-x-0 glass border-t border-white/20 z-50 safe-area-bottom">
    <div class="flex justify-around items-center h-16">
      <router-link
        to="/"
        :class="[
          'flex flex-col items-center gap-1 transition-all duration-300',
          $route.path === '/' ? 'text-orange-500' : 'text-gray-400'
        ]"
      >
        <span :class="['text-xl transition-transform', $route.path === '/' ? 'scale-110' : '']">&#127968;</span>
        <span class="text-[10px] font-medium">首页</span>
      </router-link>
      <router-link
        to="/cart"
        :class="[
          'flex flex-col items-center gap-1 transition-all duration-300',
          $route.path === '/cart' ? 'text-orange-500' : 'text-gray-400'
        ]"
      >
        <span class="text-xl relative">
          &#128722;
          <span v-if="cartCount" class="absolute -top-1 -right-2 bg-gradient-to-r from-red-500 to-orange-500 text-white text-[10px] font-bold rounded-full min-w-[16px] h-4 flex items-center justify-center px-0.5">
            {{ cartCount }}
          </span>
        </span>
        <span class="text-[10px] font-medium">购物车</span>
      </router-link>
      <router-link
        to="/coupons"
        :class="[
          'flex flex-col items-center gap-1 transition-all duration-300',
          $route.path === '/coupons' ? 'text-orange-500' : 'text-gray-400'
        ]"
      >
        <span :class="['text-xl transition-transform', $route.path === '/coupons' ? 'scale-110' : '']">🎫</span>
        <span class="text-[10px] font-medium">领券</span>
      </router-link>
      <router-link
        to="/user"
        :class="[
          'flex flex-col items-center gap-1 transition-all duration-300',
          $route.path.startsWith('/user') ? 'text-orange-500' : 'text-gray-400'
        ]"
      >
        <span :class="['text-xl transition-transform', $route.path.startsWith('/user') ? 'scale-110' : '']">&#128100;</span>
        <span class="text-[10px] font-medium">我的</span>
      </router-link>
      <router-link
        v-if="isVendor"
        to="/vendor"
        :class="[
          'flex flex-col items-center gap-1 transition-all duration-300',
          $route.path.startsWith('/vendor') ? 'text-orange-500' : 'text-gray-400'
        ]"
      >
        <span :class="['text-xl transition-transform', $route.path.startsWith('/vendor') ? 'scale-110' : '']">&#127978;</span>
        <span class="text-[10px] font-medium">商家</span>
      </router-link>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const cart = useCartStore()
const { isLoggedIn, isVendor, hasShop, user, shop } = storeToRefs(auth)
const { count: cartCount } = storeToRefs(cart)

const showDropdown = ref(false)
const isProductDetail = computed(() => route.path.startsWith('/product/'))

watch(isLoggedIn, (val) => {
  if (val) {
    cart.fetchCart().catch(() => {})
    auth.fetchShop().catch(() => {})
  }
})

function handleLogout() {
  showDropdown.value = false
  auth.logout()
  router.push('/')
}
</script>

<style scoped>
.safe-area-bottom {
  padding-bottom: env(safe-area-inset-bottom);
}

.dropdown-enter-active {
  transition: all 0.2s ease-out;
}
.dropdown-leave-active {
  transition: all 0.15s ease-in;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
