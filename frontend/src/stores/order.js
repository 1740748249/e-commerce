import { defineStore } from 'pinia'
import { orderAPI, paymentAPI } from '@/api'

export const useOrderStore = defineStore('order', {
  state: () => ({
    userOrders: [],
    shopOrders: [],
    currentDetail: null,
    loading: false,
  }),
  actions: {
    // ==================== 用户订单 ====================

    async fetchMyOrders(params) {
      this.loading = true
      try {
        const data = await orderAPI.myOrders(params)
        this.userOrders = (data?.records || data?.list || data || []).map(o => mapOrder(o))
      } catch { /* 后端未就绪 */ }
      this.loading = false
    },

    async fetchDetail(orderNo) {
      this.loading = true
      try {
        const data = await orderAPI.detail(orderNo)
        this.currentDetail = data ? {
          id: data.id,
          orderNo: data.orderNo,
          totalAmount: data.totalAmount,
          discountAmount: data.discountAmount,
          payAmount: data.payAmount,
          couponId: data.couponId,
          status: data.status,
          statusText: data.statusText,
          receiverName: data.receiverName,
          receiverPhone: data.receiverPhone,
          receiverAddr: data.receiverAddr,
          remark: data.remark,
          payTime: data.payTime,
          createTime: data.createTime,
          items: (data.items || []).map(i => ({
            productId: i.productId,
            skuId: i.skuId,
            skuName: i.skuName,
            productName: i.productName,
            productImage: i.productImage,
            price: i.price,
            quantity: i.quantity,
          })),
        } : null
      } catch { this.currentDetail = null }
      this.loading = false
    },

    // ==================== 商家订单 ====================

    async fetchShopOrders(params) {
      this.loading = true
      try {
        const data = await orderAPI.shopOrders(params)
        this.shopOrders = (data?.records || data?.list || data || []).map(o => mapOrder(o))
      } catch { /* 后端未就绪 */ }
      this.loading = false
    },

    // ==================== 下单 ====================

    async preview(items, couponId) {
      return orderAPI.preview({
        items: items.map(i => ({
          productId: i.productId,
          skuId: i.skuId || 0,
          quantity: i.qty || i.quantity || 1,
        })),
        couponId,
      })
    },

    async place(items, addressId, couponId, remark) {
      return orderAPI.create({
        items: items.map(i => ({
          productId: i.productId,
          skuId: i.skuId || 0,
          quantity: i.qty || i.quantity || 1,
        })),
        addressId,
        couponId,
        remark,
      })
    },

    async buyNow(productId, skuId, quantity, addressId, couponId, remark) {
      return await orderAPI.buyNow({ productId, skuId, quantity, addressId, couponId, remark })
    },

    // ==================== 状态操作 ====================

    async cancel(orderNo) {
      await orderAPI.cancel(orderNo)
      await this.fetchMyOrders()
    },

    // status: 2=发货, 3=完成 (数字，不是中文)
    async updateStatus(orderNo, status) {
      await orderAPI.updateStatus(orderNo, status)
      await this.fetchShopOrders()
    },

    async refund(orderNo, refundAmount, reason) {
      return await paymentAPI.refund(orderNo, refundAmount, reason)
    },
  },
})

const STATUS_MAP = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款' }

function mapOrder(o) {
  return {
    id: o.id,
    orderNo: o.orderNo,
    productName: o.items?.[0]?.productName || '',
    productImage: o.items?.[0]?.productImage || '',
    price: o.items?.[0]?.price || 0,
    qty: o.items?.[0]?.quantity || 1,
    total: o.totalAmount,
    discountAmount: o.discountAmount,
    payAmount: o.payAmount,
    status: STATUS_MAP[o.status] || o.statusText || '未知',
    statusText: o.statusText,
    couponId: o.couponId,
    buyerId: o.userId,
    buyerName: o.receiverName,
    buyerPhone: o.receiverPhone,
    buyerAddr: o.receiverAddr,
    remark: o.remark,
    shopId: o.shopId || o.items?.[0]?.shopId || 0,
    time: o.createTime,
    items: o.items,
  }
}
