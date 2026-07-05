import { defineStore } from 'pinia'
import { notificationAPI } from '@/api'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    shopNotifications: [],
    unreadCount: 0,
    page: 1,
    size: 20,
    hasMore: true,
    loading: false,
  }),
  actions: {
    async fetchShop(reset = true) {
      if (this.loading) return
      if (reset) {
        this.page = 1
        this.shopNotifications = []
        this.hasMore = true
      }
      if (!this.hasMore) return

      this.loading = true
      try {
        const data = await notificationAPI.shop({ page: this.page, size: this.size })
        const list = (data?.list || data?.records || data || [])
        const mapped = (Array.isArray(list) ? list : []).map(n => ({
          id: n.id,
          type: n.type,
          title: n.title,
          content: n.content,
          orderId: n.orderId,
          read: n.read || n.isRead,
          time: n.createTime || n.time,
          shopId: n.shopId,
        }))
        if (reset) {
          this.shopNotifications = mapped
        } else {
          this.shopNotifications.push(...mapped)
        }
        this.hasMore = mapped.length >= this.size
        this.page++
      } catch { /* ignore */ }
      this.loading = false

      if (reset) await this.fetchUnreadCount()
    },

    async loadMore() {
      await this.fetchShop(false)
    },

    async fetchUnreadCount() {
      try {
        const data = await notificationAPI.unreadCount()
        this.unreadCount = data?.count ?? data ?? this.shopNotifications.filter(n => !n.read).length
      } catch {
        this.unreadCount = this.shopNotifications.filter(n => !n.read).length
      }
    },

    async markRead(id) {
      try {
        await notificationAPI.markRead(id)
      } catch { /* ignore */ }
      const n = this.shopNotifications.find(x => x.id === id)
      if (n) { n.read = true; this.unreadCount = Math.max(0, this.unreadCount - 1) }
    },

    pushNotification(msg) {
      const exists = this.shopNotifications.some(n => n.id === msg.id)
      if (exists) return
      this.shopNotifications.unshift({
        id: msg.id,
        type: msg.type,
        title: msg.title,
        content: msg.content,
        orderId: msg.orderId,
        read: msg.read || msg.isRead || false,
        time: msg.createTime || msg.time || new Date().toISOString(),
        shopId: msg.shopId,
      })
      this.unreadCount++
    },

    async markAllRead() {
      try {
        await notificationAPI.markAllRead()
      } catch { /* ignore */ }
      this.shopNotifications.forEach(n => n.read = true)
      this.unreadCount = 0
    },
  },
})
