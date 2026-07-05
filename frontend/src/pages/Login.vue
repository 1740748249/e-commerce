<template>
  <div class="min-h-[calc(100vh-80px)] flex items-center justify-center px-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <div class="w-16 h-16 mx-auto mb-4 bg-gradient-to-br from-orange-400 to-orange-600 rounded-2xl flex items-center justify-center shadow-lg shadow-orange-200">
          <span class="text-white text-2xl font-bold">多</span>
        </div>
        <h1 class="text-2xl md:text-3xl font-bold text-gray-800">欢迎回来</h1>
        <p class="text-sm text-gray-500 mt-2">登录 <span class="text-orange-500 font-medium">多多商城</span>，发现更多好物</p>
      </div>

      <div class="bg-white rounded-3xl p-6 md:p-8 shadow-xl shadow-gray-200/50 border border-gray-100">
        <el-form ref="formRef" :model="form" :rules="rules">
          <el-form-item prop="username">
            <label class="text-sm text-gray-600 font-medium">用户名</label>
            <el-input v-model="form.username" placeholder="请输入用户名" class="!mt-2" @keyup.enter="doLogin" />
          </el-form-item>

          <el-form-item prop="password">
            <label class="text-sm text-gray-600 font-medium">密码</label>
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password class="!mt-2" @keyup.enter="doLogin" />
          </el-form-item>

          <el-form-item class="!mt-2">
            <el-button
              type="primary"
              :loading="loading"
              size="large"
              class="w-full !rounded-xl !text-sm !font-semibold !shadow-lg !shadow-orange-200"
              style="background: linear-gradient(135deg, #f97316, #f59e0b); border: none;"
              @click="doLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="flex items-center gap-4 mb-6">
          <div class="flex-1 h-px bg-gray-200"></div>
          <span class="text-xs text-gray-400">还没有账号？</span>
          <div class="flex-1 h-px bg-gray-200"></div>
        </div>

        <p class="text-center">
          <router-link to="/register" class="text-orange-500 font-semibold hover:text-orange-600 transition">立即注册</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function doLogin() {
  if (loading.value) return
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await auth.login(form.username, form.password)
    if (auth.user?.role === 2) {
      auth.logout()
      ElMessage.error('管理员请使用管理后台登录')
      loading.value = false
      return
    }
    ElMessage.success('登录成功')
    auth.fetchShop().catch(() => {})
    await nextTick()
    location.href = '/'
  } catch (e) {
    ElMessage.error(e.message || '用户名或密码错误')
  } finally {
    loading.value = false
  }
}
</script>
