import { defineStore } from 'pinia'
import { couponAPI } from '@/api'

export const useCouponStore = defineStore('coupon', {
  state: () => ({
    allCoupons: [],
    myCoupons: { available: [], used: [], expired: [] },
    loading: false,
  }),
  getters: {
    available: (s) => s.myCoupons.available,
    used: (s) => s.myCoupons.used,
    expired: (s) => s.myCoupons.expired,
  },
  actions: {
    async fetchAll() {
      this.loading = true
      try {
        const [coupons, myData] = await Promise.all([
          couponAPI.list(),
          couponAPI.myCoupons().catch(() => null),
        ])

        // 用户对每个券的已领取数（用于判"已领取"）
        const claimedCountMap = {}
        if (myData) {
          ;[...(myData.available || []), ...(myData.used || []), ...(myData.expired || [])].forEach(uc => {
            claimedCountMap[uc.couponId] = (claimedCountMap[uc.couponId] || 0) + 1
          })
        }

        this.allCoupons = (coupons || []).map(c => {
          const userClaimed = claimedCountMap[c.id] || 0
          const limit = c.limitPerUser
          return {
            id: c.id,
            name: c.name,
            type: c.type === 0 ? 'full_reduce' : 'no_threshold',
            threshold: c.threshold,
            reduce: c.reduce,
            yuanThreshold: c.threshold ? (c.threshold / 100).toFixed(0) : '0',
            yuanReduce: c.reduce ? (c.reduce / 100).toFixed(0) : '0',
            description: c.description,
            validDays: c.validDays || 7,
            categoryId: c.categoryId,
            limitPerUser: limit,
            claimedCount: c.claimedCount || 0,
            userClaimed,
            claimed: userClaimed >= (limit > 0 ? limit : 1),
          }
        })
      } catch { /* 后端未就绪 */ }
      this.loading = false
    },
    async fetchMy() {
      this.loading = true
      try {
        const data = await couponAPI.myCoupons()
        this.myCoupons = {
          available: (data?.available || []).map(mapCoupon),
          used: (data?.used || []).map(mapCoupon),
          expired: (data?.expired || []).map(mapCoupon),
        }
      } catch { /* 后端未就绪 */ }
      this.loading = false
    },
    async claim(couponId) {
      await couponAPI.claim(couponId)
      await Promise.all([this.fetchAll(), this.fetchMy()])
    },
  },
})

function mapCoupon(c) {
  return {
    id: c.id,
    couponId: c.couponId,
    couponName: c.couponName || c.name,
    name: c.couponName || c.name,
    type: c.type,
    threshold: c.threshold,
    reduce: c.reduce,
    yuanThreshold: c.threshold ? (c.threshold / 100).toFixed(0) : '0',
    yuanReduce: c.reduce ? (c.reduce / 100).toFixed(0) : '0',
    description: c.description,
    status: c.status,
    statusText: c.statusText,
    expireAt: c.expireAt,
    usedAt: c.usedAt,
  }
}
