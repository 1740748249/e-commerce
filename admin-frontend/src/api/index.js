import { get, post, put, del } from './request'

// ==================== 管理员 ====================
export const adminAPI = {
  login: (body) => post('/admin/login', body),
  statistics: () => get('/admin/statistics'),
  getUsers: (params) => get('/admin/users', params),
  updateUserStatus: (id, status) => put(`/admin/users/${id}/status`, null, { status }),
  getPendingShops: (params) => get('/admin/shops/pending', params),
  approveShop: (shopId, approved) => put(`/admin/shops/${shopId}/approve`, null, { approved }),
}

// ==================== 商品 ====================
export const productAPI = {
  getRanking: (limit = 10, opts) => get('/ranking', { limit }, opts),
  getDetail: (id, opts) => get(`/products/${id}`, null, opts),
}

// ==================== 店铺（公共） ====================
export const shopAPI = {
  list: () => get('/shops'),
  getProducts: (shopId, params) => get(`/shops/${shopId}/products`, params),
}

// ==================== 秒杀场次 - 管理端 ====================
export const flashSessionAPI = {
  list: (params) => get('/admin/flash-sessions', params),
  create: (body) => post('/admin/flash-sessions', body),
  update: (id, body) => put(`/admin/flash-sessions/${id}`, body),
  remove: (id) => del(`/admin/flash-sessions/${id}`),
}

// ==================== 优惠券 - 管理端 ====================
export const couponAdminAPI = {
  list: (params) => get('/admin/coupons', params),
  detail: (id) => get(`/admin/coupons/${id}`),
  create: (body) => post('/admin/coupons', body),
  update: (id, body) => put(`/admin/coupons/${id}`, body),
  updateStatus: (id, status) => put(`/admin/coupons/${id}/status`, null, { status }),
  remove: (id) => del(`/admin/coupons/${id}`),
}

// ==================== 秒杀报名审核 - 管理端 ====================
export const flashSaleApprovalAPI = {
  applications: (params) => get('/admin/flash-sales/applications', params),
  approve: (id, body) => put(`/admin/flash-sales/${id}/approve`, body),
}

// ==================== 订单 ====================
export const orderAPI = {
  myOrders: (params) => get('/orders/my', params),
  shopOrders: (params) => get('/orders/shop', params),
  detail: (orderNo) => get(`/orders/${orderNo}`),
  cancel: (orderNo) => put(`/orders/${orderNo}/cancel`),
  updateStatus: (orderNo, status) => put(`/orders/${orderNo}/status`, { status }),
  buyNow: (body) => post('/orders/buy-now', body),
}

// ==================== 通知 ====================
export const notificationAPI = {
  send: (body) => post('/notifications', body),
  broadcast: (title, content) => post(`/notifications/broadcast?title=${encodeURIComponent(title)}&content=${encodeURIComponent(content)}`),
}

// ==================== 支付 ====================
export const paymentAPI = {
  status: (orderNo) => get(`/payment/status/${orderNo}`),
  return: (orderNo) => get(`/payment/return`, { orderNo }),
}
