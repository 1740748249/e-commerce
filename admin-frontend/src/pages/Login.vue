<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-100 via-indigo-50 to-slate-100 px-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-indigo-600 rounded-2xl text-white text-2xl font-bold mb-4">多</div>
        <h1 class="text-2xl font-bold text-gray-900">多多商城管理后台</h1>
        <p class="text-gray-500 mt-1">请使用管理员账号登录</p>
        <router-link to="/stores" class="inline-block mt-3 text-sm text-indigo-600 hover:text-indigo-700 font-medium">
          🛒 进入商城
        </router-link>
      </div>

      <div class="bg-white rounded-2xl shadow-lg p-8">
        <form @submit.prevent="handleLogin" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">用户名</label>
            <input v-model="form.username" type="text" required
              class="w-full px-4 py-2.5 rounded-lg border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition-shadow"
              placeholder="请输入管理员用户名" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">密码</label>
            <input v-model="form.password" type="password" required
              class="w-full px-4 py-2.5 rounded-lg border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition-shadow"
              placeholder="请输入密码" />
          </div>

          <div v-if="error" class="bg-red-50 text-red-600 text-sm px-4 py-2.5 rounded-lg flex items-center gap-2">
            <span>⚠️</span> {{ error }}
          </div>

          <button type="submit" :disabled="loading"
            class="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white font-medium rounded-lg transition-colors">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>
      </div>

      <p class="text-center text-xs text-gray-400 mt-6">
        测试账号：admin / admin123
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    const result = await authStore.login(form)
    if (result.success) {
      router.push('/')
    } else {
      error.value = result.message
    }
  } catch {
    error.value = '登录失败，请检查网络或账号密码'
  } finally {
    loading.value = false
  }
}
</script>
