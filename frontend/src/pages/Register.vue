<template>
  <div class="min-h-[calc(100vh-80px)] flex items-center justify-center px-4 py-8">
    <div class="w-full max-w-md">
      <div class="text-center mb-6">
        <div class="w-16 h-16 mx-auto mb-4 bg-gradient-to-br from-orange-400 to-orange-600 rounded-2xl flex items-center justify-center shadow-lg shadow-orange-200">
          <span class="text-white text-2xl font-bold">多</span>
        </div>
        <h1 class="text-2xl md:text-3xl font-bold text-gray-800">注册账号</h1>
        <p class="text-sm text-gray-500 mt-2">加入 <span class="text-orange-500 font-medium">多多商城</span>，开启购物之旅</p>
      </div>

      <div class="bg-white rounded-3xl p-6 md:p-8 shadow-xl shadow-gray-200/50 border border-gray-100">
        <el-form ref="formRef" :model="form" :rules="rules">
          <!-- 头像上传 -->
          <div class="flex flex-col items-center mb-4">
            <div
              class="relative w-20 h-20 rounded-full bg-gray-100 border-2 border-dashed cursor-pointer overflow-hidden transition-all duration-200 hover:border-orange-400 hover:bg-orange-50"
              :class="avatarPreview ? 'border-solid border-gray-200' : 'border-gray-300'"
              @click="avatarInput.click()"
            >
              <img v-if="avatarPreview" :src="avatarPreview" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex flex-col items-center justify-center text-gray-400">
                <span class="text-lg">📷</span>
              </div>
            </div>
            <span class="text-xs text-gray-400 mt-2">上传头像</span>
            <input ref="avatarInput" type="file" accept="image/*" class="hidden" @change="onAvatarChange" />
          </div>

          <el-form-item prop="username">
            <label class="text-sm text-gray-600 font-medium">用户名</label>
            <el-input v-model="form.username" placeholder="4-20位字母或数字" maxlength="20" class="!mt-2" />
          </el-form-item>

          <el-form-item prop="password">
            <label class="text-sm text-gray-600 font-medium">密码</label>
            <el-input v-model="form.password" type="password" placeholder="至少6位密码" show-password class="!mt-2" />
          </el-form-item>

          <div class="grid grid-cols-2 gap-3">
            <el-form-item prop="name">
              <label class="text-sm text-gray-600 font-medium">姓名</label>
              <el-input v-model="form.name" placeholder="您的姓名" class="!mt-2" />
            </el-form-item>
            <el-form-item prop="phone">
              <label class="text-sm text-gray-600 font-medium">手机号</label>
              <el-input v-model="form.phone" placeholder="11位手机号码" maxlength="11" class="!mt-2" />
            </el-form-item>
          </div>

          <el-form-item class="!mt-4">
            <el-button
              type="primary"
              :loading="loading"
              size="large"
              class="w-full !rounded-xl !text-sm !font-semibold !shadow-lg !shadow-orange-200"
              style="background: linear-gradient(135deg, #f97316, #f59e0b); border: none;"
              @click="doReg"
            >
              {{ loading ? '注册中...' : '立即注册' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="flex items-center gap-4 mt-2 mb-5">
          <div class="flex-1 h-px bg-gray-200"></div>
          <span class="text-xs text-gray-400">已有账号？</span>
          <div class="flex-1 h-px bg-gray-200"></div>
        </div>
        <p class="text-center">
          <router-link to="/login" class="text-orange-500 font-semibold hover:text-orange-600 transition">立即登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { fileAPI } from '@/api'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  name: '',
  phone: '',
  avatar: '',
})

const avatarPreview = ref('')
const avatarFile = ref(null)

const validateUsername = (_rule, value, callback) => {
  if (!value) return callback(new Error('请输入用户名'))
  if (!/^[a-zA-Z0-9]{4,20}$/.test(value)) return callback(new Error('用户名需为4-20位字母或数字'))
  callback()
}

const validatePhone = (_rule, value, callback) => {
  if (!value) return callback(new Error('请输入手机号'))
  if (!/^1[3-9]\d{9}$/.test(value)) return callback(new Error('手机号格式不正确'))
  callback()
}

const rules = {
  username: [{ validator: validateUsername, trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
}

async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  avatarFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => { avatarPreview.value = ev.target.result }
  reader.readAsDataURL(file)
  try {
    const res = await fileAPI.upload(file, 'avatar')
    form.avatar = res.url
    ElMessage.success('头像上传成功')
  } catch (err) {
    ElMessage.error('头像上传失败: ' + (err.message || '未知错误'))
    avatarPreview.value = ''
    avatarFile.value = null
  }
}

async function doReg() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const payload = { ...form }
    if (!payload.avatar) delete payload.avatar
    await auth.register(payload)
    ElMessage.success('注册成功，即将跳转到登录页')
    setTimeout(() => { location.href = '/login' }, 1500)
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>
