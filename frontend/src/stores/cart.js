import { defineStore } from 'pinia'
import { cartAPI } from '@/api'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    loading: false,
  }),
  getters: {
    count: (s) => s.items.reduce((c, i) => c + (i.qty || 0), 0),
    total: (s) => s.items.reduce((c, i) => c + (i.price || 0) * (i.qty || 0), 0),
  },
  actions: {
    async fetchCart() {
      if (this.loading) return
      this.loading = true
      try {
        const data = await cartAPI.list()
        this.items = (data || []).map(item => ({
          id: item.cartItemId || item.id,
          productId: item.productId,
          skuId: item.skuId || 0,
          skuName: item.skuName || '',
          name: item.productName,
          price: item.price,
          image: item.productImage,
          qty: item.quantity,
          shopId: item.shopId,
          shopName: item.shopName,
        }))
      } catch { /* 后端未就绪 */ }
      this.loading = false
    },
    async add(item, qty = 1) {
      const exist = this.items.find(
        i => i.productId === item.productId && i.skuId === item.skuId
      )
      if (exist) {
        const prevQty = exist.qty
        exist.qty += qty
        try {
          await cartAPI.add({ productId: item.productId, skuId: item.skuId || 0, quantity: qty })
        } catch {
          exist.qty = prevQty
        }
      } else {
        const tempId = -Date.now()
        this.items.unshift({ ...item, qty, id: tempId })
        try {
          await cartAPI.add({ productId: item.productId, skuId: item.skuId || 0, quantity: qty })
          await this.fetchCart()
        } catch {
          this.items = this.items.filter(i => i.id !== tempId)
        }
      }
    },
    async updateQty(id, qty) {
      const item = this.items.find(i => i.id === id)
      if (!item) return
      const newQty = Math.max(1, qty)
      const prevQty = item.qty
      item.qty = newQty
      try {
        await cartAPI.update(id, newQty)
      } catch {
        item.qty = prevQty
      }
    },
    async remove(id) {
      const idx = this.items.findIndex(i => i.id === id)
      const removed = idx >= 0 ? this.items.splice(idx, 1)[0] : null
      try {
        await cartAPI.remove(id)
      } catch {
        if (removed) this.items.splice(idx, 0, removed)
      }
    },
    async clear() {
      try {
        await cartAPI.clear()
      } catch { /* fall through */ }
      this.items = []
    },
  },
})
