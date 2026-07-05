<template>
  <div
    v-if="auth.isVendor"
    class="px-4 md:px-0 h-[calc(100vh-8rem)] md:h-auto flex flex-col"
  >
    <!-- 标题 -->
    <div class="flex items-center justify-between mb-5 shrink-0">
      <div class="flex items-center gap-3">
        <router-link to="/vendor" class="md:hidden text-gray-400 active:text-gray-600 text-xl">&larr;</router-link>
        <h1 class="text-lg md:text-xl font-bold text-gray-800">消息通知</h1>
        <span v-if="notif.unreadCount" class="bg-red-500 text-white text-xs px-2 py-0.5 rounded-full">
          {{ notif.unreadCount }} 条未读
        </span>
      </div>
      <button
        v-if="notif.unreadCount"
        @click="notif.markAllRead()"
        class="text-sm text-orange-500 font-medium hover:text-orange-600"
      >
        全部已读
      </button>
    </div>

    <!-- 空状态 -->
    <div v-if="!notif.loading && notif.shopNotifications.length === 0" class="text-center py-24">
      <div class="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center">
        <span class="text-3xl">🔔</span>
      </div>
      <p class="text-gray-500 font-medium">暂无通知</p>
      <p class="text-sm text-gray-400 mt-1">有新订单时会收到提醒</p>
    </div>

    <!-- 通知列表 -->
    <div
      v-else
      class="flex-1 overflow-y-auto space-y-3 -mr-2 pr-2"
      @scroll="onScroll"
      ref="listRef"
    >
      <div
        v-for="n in notif.shopNotifications"
        :key="n.id"
        @click="notif.markRead(n.id)"
        class="relative bg-white rounded-2xl shadow-md cursor-pointer transition-all duration-300 hover:shadow-lg overflow-hidden"
        :class="{ 'ring-2 ring-offset-1': !n.read }"
      >
        <!-- 左侧色条 + 未读闪烁点 -->
        <div
          :class="[
            'absolute left-0 top-0 bottom-0 w-1.5',
            typeStyle(n).bar
          ]"
        ></div>
        <div v-if="!n.read" class="absolute top-4 right-4 w-2.5 h-2.5 bg-orange-500 rounded-full animate-pulse"></div>

        <!-- 单条已读按钮 -->
        <button
          v-if="!n.read"
          @click.stop="notif.markRead(n.id)"
          class="absolute bottom-3 right-4 text-xs text-orange-500 font-medium hover:text-orange-600 hover:underline"
        >
          标为已读
        </button>

        <div class="pl-6 pr-5 py-4 md:py-5">
          <!-- 头部：图标 + 类型标签 + 时间 -->
          <div class="flex items-center gap-3 mb-2">
            <span
              :class="[
                'w-9 h-9 rounded-xl flex items-center justify-center text-base shrink-0',
                typeStyle(n).badge
              ]"
            >
              {{ typeStyle(n).icon }}
            </span>
            <div class="flex-1 min-w-0">
              <h3 class="font-semibold text-sm md:text-base text-gray-800 truncate">{{ n.title }}</h3>
              <div class="flex items-center gap-2 mt-0.5">
                <span
                  :class="[
                    'text-xs px-2 py-0.5 rounded-full font-medium',
                    typeStyle(n).tag
                  ]"
                >
                  {{ typeStyle(n).label }}
                </span>
                <span class="text-xs text-gray-400">{{ n.time }}</span>
              </div>
            </div>
          </div>

          <!-- 内容 -->
          <p class="text-sm text-gray-500 leading-relaxed ml-12">{{ n.content }}</p>

          <!-- 关联订单链接 -->
          <div class="ml-12 mt-3" v-if="n.orderId">
            <router-link
              :to="`/vendor/order/${n.orderId}`"
              @click.stop
              class="inline-flex items-center gap-1 text-xs text-blue-600 font-medium hover:text-blue-700 hover:underline"
            >
              查看订单 →
            </router-link>
          </div>
        </div>
      </div>

      <!-- 底部加载 -->
      <div class="text-center py-6">
        <span v-if="notif.loading" class="text-sm text-gray-400">加载中...</span>
        <span v-else-if="!notif.hasMore && notif.shopNotifications.length > 0" class="text-sm text-gray-300">— 没有更多了 —</span>
      </div>
    </div>
  </div>

  <!-- 非商家 -->
  <div v-else class="text-center py-24">
    <p class="text-gray-400">请使用商家账号<a href="/login" class="text-orange-500">登录</a></p>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const auth = useAuthStore()
const notif = useNotificationStore()
const listRef = ref(null)

const typeMap = {
  0: { icon: '📦', label: '新订单',   bar: 'bg-blue-500',   badge: 'bg-blue-50 text-blue-600',     tag: 'bg-blue-50 text-blue-700' },
  1: { icon: '🔔', label: '系统通知', bar: 'bg-indigo-500', badge: 'bg-indigo-50 text-indigo-600', tag: 'bg-indigo-50 text-indigo-700' },
  2: { icon: '🎉', label: '促销活动', bar: 'bg-orange-500', badge: 'bg-orange-50 text-orange-600', tag: 'bg-orange-50 text-orange-700' },
  broadcast: { icon: '📢', label: '全站广播', bar: 'bg-red-500', badge: 'bg-red-50 text-red-600', tag: 'bg-red-50 text-red-700' },
}

function typeStyle(n) {
  if (n.shopId === 0 || n.shopId === '0') return typeMap.broadcast
  return typeMap[n.type] || typeMap[1]
}

function onScroll(e) {
  const el = e.target
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 60) {
    notif.loadMore()
  }
}

onMounted(async () => {
  await notif.fetchShop(true)
  await nextTick()
  if (listRef.value && listRef.value.scrollHeight <= listRef.value.clientHeight && notif.hasMore) {
    notif.loadMore()
  }
})
</script>
