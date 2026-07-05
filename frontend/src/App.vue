<template>
  <div class="min-h-screen bg-gradient-to-br from-gray-50 via-orange-50/30 to-amber-50/20">
    <Navbar />
    <main class="max-w-7xl mx-auto px-0 md:px-4 pt-0 md:pt-2 pb-20 md:pb-6">
      <router-view v-slot="{ Component }">
        <keep-alive include="Home">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>

    <!-- WebSocket 实时通知弹窗 -->
    <Teleport to="body">
      <TransitionGroup
        name="toast"
        tag="div"
        class="fixed top-5 right-5 z-[9999] flex flex-col gap-3 w-80"
      >
        <div
          v-for="t in toasts"
          :key="t.id"
          class="relative bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden"
        >
          <!-- 左侧色条 -->
          <div
            :class="[
              'absolute left-0 top-0 bottom-0 w-1',
              t.type === 0 ? 'bg-blue-500' :
              t.type === 1 ? 'bg-indigo-500' :
              t.type === 2 ? 'bg-orange-500' :
              'bg-red-500'
            ]"
          ></div>
          <!-- 内容 -->
          <div class="pl-5 pr-10 py-4">
            <div class="flex items-center gap-3 mb-1">
              <span
                :class="[
                  'w-9 h-9 rounded-xl flex items-center justify-center text-base shrink-0',
                  t.type === 0 ? 'bg-blue-50 text-blue-600' :
                  t.type === 1 ? 'bg-indigo-50 text-indigo-600' :
                  t.type === 2 ? 'bg-orange-50 text-orange-600' :
                  'bg-red-50 text-red-600'
                ]"
              >
                {{ t.type === 0 ? '📦' : t.type === 1 ? '🔔' : t.type === 2 ? '🎉' : '📢' }}
              </span>
              <div class="min-w-0">
                <p class="font-semibold text-sm text-gray-800 truncate">{{ t.title }}</p>
                <p class="text-xs text-gray-400 mt-0.5">
                  {{ t.type === 'broadcast' ? '📢 全站广播' : t.type === 0 ? '新订单通知' : t.type === 1 ? '系统通知' : '促销活动' }}
                </p>
              </div>
            </div>
            <p class="text-sm text-gray-600 leading-relaxed mt-2 line-clamp-3">{{ t.content }}</p>
          </div>
          <!-- X 关闭按钮 -->
          <button
            @click="dismissToast(t.id)"
            class="absolute top-3 right-3 w-6 h-6 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-400 hover:text-gray-600 flex items-center justify-center text-xs transition-colors"
          >
            ✕
          </button>
        </div>
      </TransitionGroup>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import Navbar from '@/components/Navbar.vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { connectWebSocket, disconnectWebSocket } from '@/composables/useWebSocket'

const auth = useAuthStore()
const notif = useNotificationStore()
const { token, isVendor } = storeToRefs(auth)
const { shop } = storeToRefs(auth)

const toasts = ref([])
let toastId = 0

function showToast(msg) {
  const id = ++toastId
  toasts.value.push({ id, title: msg.title, content: msg.content, type: msg.type })
}

function dismissToast(id) {
  const idx = toasts.value.findIndex(t => t.id === id)
  if (idx >= 0) toasts.value.splice(idx, 1)
}

function onNotification(msg) {
  notif.pushNotification(msg)
  showToast(msg)
}

function onBroadcast(msg) {
  showToast(msg)
}

onMounted(async () => {
  if (auth.isLoggedIn) {
    await auth.fetchShop().catch(() => {})
    tryConnect()
  }
})

watch([token, shop, isVendor], () => tryConnect(), { immediate: true })

function tryConnect() {
  if (token.value && isVendor.value && shop.value?.id) {
    connectWebSocket(token.value, shop.value.id, onNotification, onBroadcast)
  } else {
    disconnectWebSocket()
  }
}

onUnmounted(() => disconnectWebSocket())
</script>

<style>
.toast-enter-active { transition: all 0.4s ease-out; }
.toast-leave-active { transition: all 0.3s ease-in; }
.toast-enter-from { opacity: 0; transform: translateX(80px); }
.toast-leave-to { opacity: 0; transform: translateX(80px); }
</style>
