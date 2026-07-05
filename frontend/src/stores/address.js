import { defineStore } from 'pinia'
import { addressAPI } from '@/api'

export const useAddressStore = defineStore('address', {
  state: () => ({
    addresses: [],
    loading: false,
  }),
  getters: {
    defaultAddress: (s) => s.addresses.find(a => a.isDefault) || s.addresses[0] || null,
  },
  actions: {
    async fetchList() {
      this.loading = true
      try {
        const data = await addressAPI.list()
        this.addresses = (data || []).map(a => ({
          id: a.id,
          name: a.receiverName,
          phone: a.receiverPhone,
          province: a.province,
          city: a.city,
          district: a.district,
          detail: a.detail,
          isDefault: a.isDefault,
          label: [a.province, a.city, a.district, a.detail].filter(Boolean).join(' '),
        }))
      } catch { /* 后端未就绪 */ }
      this.loading = false
    },
    async getById(id) {
      const data = await addressAPI.getById(id)
      return {
        id: data.id,
        name: data.receiverName,
        phone: data.receiverPhone,
        province: data.province,
        city: data.city,
        district: data.district,
        detail: data.detail,
        isDefault: data.isDefault,
        label: [data.province, data.city, data.district, data.detail].filter(Boolean).join(' '),
      }
    },
    async add(body) {
      await addressAPI.add(body)
      await this.fetchList()
    },
    async update(id, body) {
      await addressAPI.update(id, body)
      await this.fetchList()
    },
    async remove(id) {
      await addressAPI.remove(id)
      this.addresses = this.addresses.filter(a => a.id !== id)
    },
  },
})
