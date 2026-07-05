<template>
  <div v-if="auth.isLoggedIn" class="max-w-lg mx-auto px-4 md:px-0">
    <button @click="$router.back()" class="text-gray-400 hover:text-gray-600 text-sm mb-4 flex items-center gap-1 transition-colors">&larr; 返回</button>
    <h1 class="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2"><span class="text-2xl">&#128100;</span> 个人信息</h1>

    <div class="bg-white rounded-2xl p-5 md:p-6 shadow-md mb-5">
      <h2 class="text-base font-semibold text-gray-800 mb-5 flex items-center gap-2"><span class="w-1 h-5 bg-orange-500 rounded-full"></span>基本资料</h2>
      <div class="space-y-4">
        <div class="flex items-center justify-center mb-6">
          <div class="relative">
            <div class="w-20 h-20 bg-gradient-to-br from-orange-400 to-orange-600 rounded-2xl flex items-center justify-center text-3xl text-white font-bold shadow-lg shadow-orange-200">{{ edit.name[0] || '?' }}</div>
            <div class="absolute -bottom-1 -right-1 w-7 h-7 bg-white rounded-full flex items-center justify-center shadow-md border border-gray-100"><span class="text-sm">&#128247;</span></div>
          </div>
        </div>
        <div>
          <label class="text-xs text-gray-400 font-medium block mb-1.5">用户名</label>
          <input v-model="edit.username" class="w-full border-0 bg-gray-100 rounded-xl px-4 py-3 text-sm text-gray-500 cursor-not-allowed" disabled />
        </div>
        <div>
          <label class="text-xs text-gray-400 font-medium block mb-1.5">姓名</label>
          <input v-model="edit.name" class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all" placeholder="请输入姓名" />
        </div>
        <div>
          <label class="text-xs text-gray-400 font-medium block mb-1.5">手机号</label>
          <input v-model="edit.phone" class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all" placeholder="请输入手机号" />
        </div>
        <button @click="saveProfile" class="w-full py-3.5 btn-primary text-white rounded-xl text-sm font-semibold mt-2 active:scale-[0.98] transition-transform">保存修改</button>
      </div>
      <p v-if="saveMsg" class="text-green-500 text-sm text-center mt-4 flex items-center justify-center gap-1"><span>&#10003;</span> {{ saveMsg }}</p>
    </div>

    <div class="bg-white rounded-2xl p-5 md:p-6 shadow-md mb-5">
      <h2 class="text-base font-semibold text-gray-800 mb-5 flex items-center gap-2"><span class="w-1 h-5 bg-orange-500 rounded-full"></span>修改密码</h2>
      <div class="space-y-4">
        <div>
          <label class="text-xs text-gray-400 font-medium block mb-1.5">当前密码</label>
          <input v-model="pwd.old" type="password" class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all" placeholder="请输入当前密码" />
        </div>
        <div>
          <label class="text-xs text-gray-400 font-medium block mb-1.5">新密码</label>
          <input v-model="pwd.new1" type="password" class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all" placeholder="至少 6 位密码" />
        </div>
        <div>
          <label class="text-xs text-gray-400 font-medium block mb-1.5">确认新密码</label>
          <input v-model="pwd.new2" type="password" class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all" placeholder="请再次输入新密码" />
        </div>
        <p v-if="pwdMsg" :class="['text-sm flex items-center gap-1', pwdOk ? 'text-green-500' : 'text-red-500']"><span>{{ pwdOk ? '&#10003;' : '&#9888;' }}</span> {{ pwdMsg }}</p>
        <button @click="changePwd" class="w-full py-3.5 border-2 border-orange-400 text-orange-500 rounded-xl text-sm font-semibold hover:bg-orange-50 transition-colors">修改密码</button>
      </div>
    </div>
  </div>

  <div v-else class="text-center py-24">
    <div class="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center"><span class="text-3xl">&#128100;</span></div>
    <p class="text-gray-500 font-medium">请先登录</p>
    <router-link to="/login" class="inline-block mt-4 px-6 py-2.5 bg-orange-500 text-white rounded-xl text-sm font-medium">去登录</router-link>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const saveMsg = ref('')
const edit = reactive({
  username: auth.user?.username || '',
  name: auth.user?.name || '',
  phone: auth.user?.phone || '',
})

const pwd = reactive({ old: '', new1: '', new2: '' })
const pwdMsg = ref('')
const pwdOk = ref(false)

async function saveProfile() {
  try {
    await auth.updateProfile({ name: edit.name, phone: edit.phone })
    saveMsg.value = '保存成功'
    setTimeout(() => saveMsg.value = '', 2000)
  } catch (e) {
    saveMsg.value = ''
    alert('保存失败：' + (e.message || '请稍后重试'))
  }
}

async function changePwd() {
  pwdMsg.value = ''
  if (pwd.new1.length < 6) {
    pwdMsg.value = '新密码至少 6 位'
    return
  }
  if (pwd.new1 !== pwd.new2) {
    pwdMsg.value = '两次密码输入不一致'
    return
  }
  try {
    await auth.changePassword({ oldPassword: pwd.old, newPassword: pwd.new1 })
    pwdMsg.value = '密码修改成功'
    pwdOk.value = true
    pwd.old = ''; pwd.new1 = ''; pwd.new2 = ''
  } catch (e) {
    pwdMsg.value = e.message || '修改失败'
    pwdOk.value = false
  }
}
</script>
