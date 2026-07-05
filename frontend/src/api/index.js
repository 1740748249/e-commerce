import { get, post, put, del, upload } from './request'

// ==================== 用户服务 (UserController → e_user 表) ====================
export const userAPI = {
  login: (body) => post('/users/login', body),
  register: (body) => post('/users/register', body),
  getMe: () => get('/users/me'),
  updateProfile: (body) => put('/users/me', body),
  changePassword: (body) => put('/users/password', body),
}

// ==================== 收货地址 (AddressController → e_user_address 表) ====================
export const addressAPI = {
  list: () => get('/addresses'),
  getById: (id) => get(`/addresses/${id}`),
  add: (body) => post('/addresses', body),
  update: (id, body) => put(`/addresses/${id}`, body),
  remove: (id) => del(`/addresses/${id}`),
}

// ==================== 店铺 (ShopController → e_shop 表) ====================
export const shopAPI = {
  apply: (body) => post('/shops/apply', body),
  myShop: () => get('/shops/me'),
}

// ==================== 管理员 (AdminController) ====================
export const adminAPI = {
  login: (body) => post('/admin/login', body),
  users: (params) => get('/admin/users', params),
  updateStatus: (id, status) => put(`/admin/users/${id}/status`, null, { status }),
  pendingShops: (params) => get('/admin/shops/pending', params),
  approveShop: (shopId, approved) => put(`/admin/shops/${shopId}/approve`, null, { approved }),
  statistics: () => get('/admin/statistics'),
}

// ==================== 商品服务 (ProductController → e_product 表) ====================
export const productAPI = {
  list: (params) => get('/products', params),
  detail: (id) => get(`/products/${id}`),
  add: (body) => post('/products', body),
  update: (id, body) => put(`/products/${id}`, body),
  updateStatus: (id, status) => put(`/products/${id}/status`, null, { status }),
  myProducts: (params) => get('/products/my', params),
}

// ==================== 分类 (CategoryController → e_category 表) ====================
export const categoryAPI = {
  list: () => get('/categories'),
}

// ==================== 店铺公开信息 (ShopController → e_shop 表) ====================
export const shopPublicAPI = {
  list: () => get('/shops'),
  products: (shopId, params) => get(`/shops/${shopId}/products`, params),
}

// ==================== 秒杀 - 用户端 ====================
export const flashSaleAPI = {
  list: (sessionId) => get('/flash-sales', sessionId ? { sessionId } : {}),
  order: (id, params) => post(`/flash-sales/${id}/order?${new URLSearchParams(params)}`),
  result: (id) => get(`/flash-sales/${id}/result`),
}

// ==================== 秒杀场次 - 管理端 ====================
export const adminFlashSessionAPI = {
  list: (params) => get('/admin/flash-sessions', params),
  create: (body) => post('/admin/flash-sessions', body),
  update: (id, body) => put(`/admin/flash-sessions/${id}`, body),
  remove: (id) => del(`/admin/flash-sessions/${id}`),
}

// ==================== 秒杀报名 - 管理端 ====================
export const adminFlashSaleAPI = {
  applications: (params) => get('/admin/flash-sales/applications', params),
  approve: (id, body) => put(`/admin/flash-sales/${id}/approve`, body),
}

// ==================== 秒杀 - 商家端 ====================
export const merchantFlashSessionAPI = {
  available: () => get('/merchant/flash-sessions'),
}

export const merchantFlashSaleAPI = {
  apply: (body) => post('/merchant/flash-sales', body),
  myApplications: (params) => get('/merchant/flash-sales/applications', params),
}

// ==================== 排行榜 (RankingController) ====================
export const rankingAPI = {
  list: (limit) => get('/ranking', { limit }),
}

// ==================== 购物车 (CartController → e_cart 表) ====================
export const cartAPI = {
  list: () => get('/cart'),
  add: (body) => post('/cart', body),
  update: (cartItemId, quantity) => put(`/cart/${cartItemId}`, { quantity }),
  remove: (cartItemId) => del(`/cart/${cartItemId}`),
  clear: () => del('/cart'),
}

// ==================== 订单 (OrderController → e_order 表) ====================
export const orderAPI = {
  create: (body) => post('/orders', body),
  buyNow: (body) => post('/orders/buy-now', body),
  preview: (body) => post('/orders/preview', body),
  myOrders: (params) => get('/orders/my', params),
  detail: (orderNo) => get(`/orders/${orderNo}`),
  cancel: (orderNo) => put(`/orders/${orderNo}/cancel`),
  shopOrders: (params) => get('/orders/shop', params),
  updateStatus: (orderNo, status) => put(`/orders/${orderNo}/status`, { status }),
  payCallback: (orderNo, body) => post(`/orders/${orderNo}/pay-callback`, body),
}

// ==================== 优惠券 (CouponController → e_coupon 表) ====================
export const couponAPI = {
  list: () => get('/coupons'),
  claim: (couponId) => post(`/coupons/${couponId}/claim`),
  myCoupons: () => get('/coupons/my'),
}

// ==================== 通知 (NotificationController → e_notification 表) ====================
export const notificationAPI = {
  shop: (params) => get('/notifications/shop', params),
  unreadCount: () => get('/notifications/unread-count'),
  markRead: (id) => put(`/notifications/${id}/read`),
  markAllRead: () => put('/notifications/read-all'),
  send: (body) => post('/notifications', body),
  broadcast: (title, content) => post(`/notifications/broadcast?title=${encodeURIComponent(title)}&content=${encodeURIComponent(content)}`),
}

// ==================== 文件服务 (FileController) ====================
export const fileAPI = {
  upload: (file, type) => upload('/files/upload', file, type),
}

// ==================== 支付 (PaymentController) ====================
export const paymentAPI = {
  pay: (orderNo) => fetch(`http://localhost:8080/payment/pay?orderNo=${orderNo}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${localStorage.getItem('token') || ''}` },
  }),
  status: (orderNo) => get(`/payment/status/${orderNo}`),
  return: (orderNo) => get(`/payment/return`, { orderNo }),
  refund: (orderNo, refundAmount, reason) => post('/payment/refund', { orderNo, refundAmount, reason }),
  refundStatus: (outRequestNo) => get(`/payment/refund/${outRequestNo}`),
}
