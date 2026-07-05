import { defineStore } from 'pinia'
import { userAPI, shopAPI } from '@/api'

function parseStoredJSON(key) {
  const raw = localStorage.getItem(key)
  if (!raw || raw === 'undefined') return null
  try { return JSON.parse(raw) } catch { return null }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: parseStoredJSON('user'),
    token: localStorage.getItem('token') || '',
    shop: null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isVendor: (s) => s.user?.role === 1,
    isAdmin: (s) => s.user?.role === 2,
    hasShop: (s) => !!s.shop && s.shop.approved === 1,
  },
  actions: {
    async login(username, password) {
      const data = await userAPI.login({ username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      return data.user
    },
    async register(form) {
      return await userAPI.register(form)
    },
    async fetchMe() {
      const data = await userAPI.getMe()
      this.user = data
      localStorage.setItem('user', JSON.stringify(data))
    },
    async updateProfile(body) {
      await userAPI.updateProfile(body)
      await this.fetchMe()
    },
    async changePassword(body) {
      await userAPI.changePassword(body)
    },
    async fetchShop() {
      try {
        const res = await shopAPI.myShop()
        this.shop = res
      } catch {
        this.shop = null
      }
    },
    async applyShop(body) {
      await shopAPI.apply(body)
      await this.fetchShop()
    },
    logout() {
      this.user = null
      this.token = ''
      this.shop = null
      localStorage.removeItem('user')
      localStorage.removeItem('token')
    },
  },
})
