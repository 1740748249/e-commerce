import { defineStore } from 'pinia'
import { productAPI, categoryAPI, shopPublicAPI, flashSaleAPI, rankingAPI } from '@/api'

export const useProductStore = defineStore('product', {
  state: () => ({
    list: [],
    total: 0,
    categories: [],
    shops: [],
    flashSales: [],
    currentDetail: null,
    loading: false,
  }),
  getters: {
    categoryOptions: (s) => s.categories.map(c => c.name),
    categoryMap: (s) => {
      const map = {}
      s.categories.forEach(c => { map[c.name] = c.id })
      return map
    },
  },
  actions: {
    async fetchList(params = {}) {
      this.loading = true
      try {
        const data = await productAPI.list(params)
        this.list = (data?.list || data || []).map(p => ({
          id: p.id, name: p.name,
          categoryId: p.categoryId, category: p.categoryName,
          shopId: p.shopId, shopName: p.shopName,
          price: p.minPrice, stock: p.totalStock,
          image: p.image, desc: p.description,
          sales: p.sales, status: p.status ? 'on' : 'off',
        }))
        this.total = data?.total || this.list.length
      } catch { /* 后端未就绪 */ }
      this.loading = false
    },
    async fetchDetail(id) {
      try {
        const p = await productAPI.detail(id)
        if (!p) return null
        this.currentDetail = {
          id: p.id, name: p.name,
          categoryId: p.categoryId, category: p.categoryName,
          shopId: p.shopId, shopName: p.shopName,
          price: p.minPrice, stock: p.totalStock,
          image: p.image, desc: p.description,
          sales: p.sales, status: p.status ? 'on' : 'off',
          skus: (p.skus || []).map(s => ({
            id: s.id, product_id: s.productId, sku_code: s.skuCode,
            specs: Array.isArray(s.specs)
              ? s.specs.reduce((acc, sp) => { acc[sp.name] = sp.value; return acc }, {})
              : s.specs,
            price: s.price, stock: s.stock, image: s.image, status: s.status,
          })),
          specGroups: p.skus ? extractSpecGroups(p.skus) : [],
        }
        return this.currentDetail
      } catch { return null }
    },
    async fetchCategories() {
      try {
        const data = await categoryAPI.list()
        this.categories = (data || []).map(c => ({ id: c.id, name: c.name }))
      } catch { /* 后端未就绪 */ }
    },
    async fetchShops() {
      try {
        this.shops = await shopPublicAPI.list() || []
      } catch { /* 后端未就绪 */ }
    },
    async fetchMyProducts(params) {
      this.loading = true
      try {
        const data = await productAPI.myProducts(params)
        this.list = (data?.list || data || []).map(p => ({
          id: p.id, name: p.name,
          categoryId: p.categoryId, category: p.categoryName,
          shopId: p.shopId, shopName: p.shopName,
          price: p.minPrice, stock: p.totalStock,
          image: p.image, desc: p.description,
          sales: p.sales, status: p.status ? 'on' : 'off',
          skus: p.skus || [],
        }))
        this.total = data?.total || this.list.length
      } catch { /* 后端未就绪 */ }
      this.loading = false
    },
    async fetchShopProducts(shopId, params) {
      this.loading = true
      try {
        const data = await shopPublicAPI.products(shopId, params)
        this.list = (data?.list || data || []).map(p => ({
          id: p.id, name: p.name,
          categoryId: p.categoryId, category: p.categoryName,
          shopId: p.shopId, shopName: p.shopName,
          price: p.minPrice, stock: p.totalStock,
          image: p.image, desc: p.description,
          sales: p.sales, status: p.status ? 'on' : 'off',
        }))
        this.total = data?.total || this.list.length
      } catch { /* 后端未就绪 */ }
      this.loading = false
    },
    async fetchFlashSales() {
      try {
        const data = await flashSaleAPI.list()
        const list = Array.isArray(data) ? data : (data ? [data] : [])
        this.flashSales = list.map(s => ({
          sessionId: s.id,
          sessionName: s.name,
          sessionStartTime: s.startTime,
          sessionEndTime: s.endTime,
          items: (s.items || []).map(f => ({
            id: f.id, productId: f.productId,
            name: f.productName, image: f.productImage,
            originalPrice: f.originalPrice, flashPrice: f.flashPrice,
            stock: f.stock, sold: f.sold,
            shopId: f.shopId, shopName: f.shopName,
            progress: f.progress,
            perUserLimit: f.perUserLimit || 1,
          })),
        }))
      } catch { /* 后端未就绪 */ }
    },
    // 商家操作
    async addProduct(body) {
      await productAPI.add(body)
      await this.fetchMyProducts()
    },
    async updateProduct(id, body) {
      await productAPI.update(id, body)
      await this.fetchMyProducts()
    },
    async deleteProduct(id) {
      await productAPI.updateStatus(id, 0)
      await this.fetchMyProducts()
    },
    async toggleStatus(id, currentStatus) {
      await productAPI.updateStatus(id, currentStatus === 'on' ? 0 : 1)
      await this.fetchMyProducts()
    },
  },
})

function extractSpecGroups(skus) {
  if (!skus || !skus.length) return []
  const first = skus[0].specs
  if (!first) return []
  const specEntries = Array.isArray(first)
    ? first.map(s => [s.name, [s.value]])
    : Object.entries(first).map(([k, v]) => [k, [v]])
  const groups = specEntries.map(([name]) => ({ name, options: [] }))
  const optionSets = groups.map(() => new Set())
  skus.forEach(sku => {
    const specs = Array.isArray(sku.specs)
      ? sku.specs
      : Object.entries(sku.specs || {}).map(([name, value]) => ({ name, value }))
    specs.forEach((sp, i) => {
      if (optionSets[i]) optionSets[i].add(sp.value)
    })
  })
  groups.forEach((g, i) => { g.options = [...optionSets[i]] })
  return groups
}
