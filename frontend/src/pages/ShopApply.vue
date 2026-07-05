<template>
  <div class="min-h-[calc(100vh-80px)] flex items-center justify-center px-4 py-8">
    <div class="w-full max-w-md">
      <div class="text-center mb-6">
        <div class="w-16 h-16 mx-auto mb-4 bg-gradient-to-br from-orange-400 to-orange-600 rounded-2xl flex items-center justify-center shadow-lg shadow-orange-200">
          <span class="text-white text-2xl">🏪</span>
        </div>
        <h1 class="text-2xl md:text-3xl font-bold text-gray-800">申请开店</h1>
        <p class="text-sm text-gray-500 mt-2">填写店铺信息，提交后等待管理员审批</p>
      </div>

      <div class="bg-white rounded-3xl p-6 md:p-8 shadow-xl shadow-gray-200/50 border border-gray-100">
        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="doApply">
          <!-- 店铺LOGO上传 -->
          <div class="flex flex-col items-center mb-4">
            <div
              class="relative w-24 h-24 rounded-2xl bg-gray-100 border-2 border-dashed cursor-pointer overflow-hidden transition-all duration-200 hover:border-orange-400 hover:bg-orange-50"
              :class="logoPreview ? 'border-solid border-gray-200' : 'border-gray-300'"
              @click="$refs.logoInput.click()"
            >
              <img v-if="logoPreview" :src="logoPreview" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex flex-col items-center justify-center text-gray-400">
                <span class="text-lg">🏪</span>
              </div>
            </div>
            <span class="text-xs text-gray-400 mt-2">店铺 LOGO</span>
            <input ref="logoInput" type="file" accept="image/*" class="hidden" @change="onLogoChange" />
          </div>

          <el-form-item prop="name">
            <label class="text-sm text-gray-600 font-medium">店铺名称</label>
            <el-input v-model="form.name" placeholder="给您的店铺起个好名字" maxlength="50" class="!mt-2" />
          </el-form-item>

          <el-form-item class="!mt-4">
            <el-button
              type="primary"
              native-type="submit"
              :loading="loading"
              size="large"
              class="w-full !rounded-xl !text-sm !font-semibold !shadow-lg !shadow-orange-200"
              style="background: linear-gradient(135deg, #f97316, #f59e0b); border: none;"
            >
              {{ loading ? '提交中...' : '提交申请' }}
            </el-button>
          </el-form-item>
        </el-form>

        <p class="text-center mt-4">
          <router-link to="/" class="text-gray-400 text-sm hover:text-orange-500 transition">返回首页</router-link>
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

const form = reactive({ name: '', logo: '' })
const logoPreview = ref('')
const logoFile = ref(null)

const rules = {
  name: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
}

async function onLogoChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  logoFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => { logoPreview.value = ev.target.result }
  reader.readAsDataURL(file)
  try {
    const res = await fileAPI.upload(file, 'logo')
    form.logo = res.url
    ElMessage.success('Logo上传成功')
  } catch (err) {
    ElMessage.error('Logo上传失败: ' + (err.message || '未知错误'))
    logoPreview.value = ''
    logoFile.value = null
  }
}

async function doApply() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const payload = { name: form.name.trim() }
    if (form.logo) payload.logo = form.logo
    await auth.applyShop(payload)
    ElMessage.success('申请已提交，请等待管理员审批')
    router.push('/user')
  } catch (e) {
    ElMessage.error(e.message || '申请失败')
  } finally {
    loading.value = false
  }
}
</script>
