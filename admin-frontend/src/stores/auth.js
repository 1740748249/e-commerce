import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { adminAPI } from '@/api'

function parseStoredJSON(key) {
  const raw = localStorage.getItem(key)
  if (!raw || raw === 'undefined') return null
  try { return JSON.parse(raw) } catch { return null }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const adminUser = ref(parseStoredJSON('admin_user'))

  const isLoggedIn = computed(() => !!token.value)

  async function login(credentials) {
    const data = await adminAPI.login(credentials)
    if (!data || !data.token) return { success: false, message: '登录失败' }

    const user = data.user
    if (!user || user.role !== 2) return { success: false, message: '仅限管理员登录，普通用户请使用商城前台' }

    token.value = data.token
    adminUser.value = user
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_user', JSON.stringify(user))
    return { success: true }
  }

  function logout() {
    token.value = ''
    adminUser.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }

  return { token, adminUser, isLoggedIn, login, logout }
})
