import { defineStore } from 'pinia'

export const useCheckoutStore = defineStore('checkout', {
  state: () => ({
    /** @type {{ productId: number, skuId: number, skuName: string, name: string, price: number, image: string, quantity: number, shopId: number, shopName: string }[]} */
    items: [],
  }),
  actions: {
    setBuyNowItem(item) {
      this.items = [{ ...item, quantity: item.quantity || 1 }]
    },
    setCartItems(items) {
      this.items = items.map(i => ({
        productId: i.productId,
        skuId: i.skuId || 0,
        skuName: i.skuName || '',
        name: i.name,
        price: i.price,
        image: i.image,
        quantity: i.qty || i.quantity || 1,
        shopId: i.shopId || 0,
        shopName: i.shopName || '',
      }))
    },
    clear() {
      this.items = []
    },
  },
  getters: {
    total: (s) => s.items.reduce((sum, i) => sum + i.price * i.quantity, 0),
    count: (s) => s.items.reduce((sum, i) => sum + i.quantity, 0),
    hasItems: (s) => s.items.length > 0,
  },
})
