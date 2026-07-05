// ============================================================
// 管理端 Mock 数据
// ============================================================

// 管理员账号
const admins = [
  { id: 1, username: 'admin', password: 'admin123', name: '系统管理员', role: 2, status: 1 },
]

// 商家（待审批 + 已审批）
const merchants = [
  { id: 12, username: 'shop_li', name: '李老板', phone: '13900001111', role: 1, shopId: 1, shopName: '数码旗舰店', status: 0, createTime: '2026-06-14 10:30:00' },
  { id: 13, username: 'shop_wang', name: '王老板', phone: '13900002222', role: 1, shopId: 2, shopName: '品质女装店', status: 0, createTime: '2026-06-15 09:15:00' },
  { id: 14, username: 'shop_zhao', name: '赵老板', phone: '13900003333', role: 1, shopId: 3, shopName: '美妆护肤坊', status: 0, createTime: '2026-06-15 14:20:00' },
  { id: 15, username: 'shop_chen', name: '陈老板', phone: '13900004444', role: 1, shopId: 4, shopName: '零食量贩店', status: 1, createTime: '2026-06-10 08:00:00' },
  { id: 16, username: 'shop_liu', name: '刘老板', phone: '13900005555', role: 1, shopId: 5, shopName: '家居生活馆', status: 1, createTime: '2026-06-11 10:00:00' },
  { id: 17, username: 'shop_zhou', name: '周老板', phone: '13900006666', role: 1, shopId: 6, shopName: '运动装备库', status: 1, createTime: '2026-06-12 11:30:00' },
  { id: 18, username: 'shop_wu', name: '吴老板', phone: '13900007777', role: 1, shopId: 7, shopName: '图书文化社', status: 1, createTime: '2026-06-13 09:45:00' },
  { id: 19, username: 'shop_sun', name: '孙老板', phone: '13900008888', role: 1, shopId: 8, shopName: '母婴优选', status: 1, createTime: '2026-06-13 16:00:00' },
  { id: 20, username: 'shop_ma', name: '马老板', phone: '13900009999', role: 1, shopId: 9, shopName: '户外探险家', status: 1, createTime: '2026-06-14 07:30:00' },
]

// 普通用户
const users = [
  { id: 2, username: 'zhangsan', name: '张三', phone: '13800000001', role: 0, shopName: null, status: 1, lastLoginTime: '2026-06-16 10:30:00', createTime: '2026-06-01 08:00:00' },
  { id: 3, username: 'lisi', name: '李四', phone: '13800000002', role: 0, shopName: null, status: 1, lastLoginTime: '2026-06-16 11:00:00', createTime: '2026-06-02 09:00:00' },
  { id: 4, username: 'wangwu', name: '王五', phone: '13800000003', role: 0, shopName: null, status: 0, lastLoginTime: '2026-06-10 15:00:00', createTime: '2026-06-03 10:00:00' },
  { id: 5, username: 'zhaoliu', name: '赵六', phone: '13800000004', role: 0, shopName: null, status: 1, lastLoginTime: '2026-06-15 09:20:00', createTime: '2026-06-04 11:00:00' },
  { id: 6, username: 'sunqi', name: '孙七', phone: '13800000005', role: 0, shopName: null, status: 1, lastLoginTime: '2026-06-14 14:00:00', createTime: '2026-06-05 12:00:00' },
  { id: 7, username: 'zhouba', name: '周八', phone: '13800000006', role: 0, shopName: null, status: 1, lastLoginTime: '2026-06-13 16:30:00', createTime: '2026-06-06 13:00:00' },
  { id: 8, username: 'wujiu', name: '吴九', phone: '13800000007', role: 0, shopName: null, status: 0, lastLoginTime: '2026-06-08 10:00:00', createTime: '2026-06-07 14:00:00' },
]

// 销量排行榜
const rankings = [
  { rank: 1, productId: 1, productName: 'iPhone 15 Pro Max', productImage: '', minPrice: 899900, sales: 3280, shopName: '数码旗舰店' },
  { rank: 2, productId: 7, productName: 'AirPods Pro 2', productImage: '', minPrice: 189900, sales: 2560, shopName: '数码旗舰店' },
  { rank: 3, productId: 2, productName: 'MacBook Pro 14', productImage: '', minPrice: 1499900, sales: 1890, shopName: '数码旗舰店' },
  { rank: 4, productId: 18, productName: '三只松鼠坚果礼盒', productImage: '', minPrice: 12800, sales: 1650, shopName: '零食量贩店' },
  { rank: 5, productId: 13, productName: '兰蔻精华肌底液', productImage: '', minPrice: 89900, sales: 1420, shopName: '美妆护肤坊' },
  { rank: 6, productId: 25, productName: '耐克跑鞋 Air Zoom', productImage: '', minPrice: 89900, sales: 1280, shopName: '运动装备库' },
  { rank: 7, productId: 10, productName: '韩版宽松卫衣', productImage: '', minPrice: 12900, sales: 1150, shopName: '品质女装店' },
  { rank: 8, productId: 4, productName: 'Sony WH-1000XM5', productImage: '', minPrice: 249900, sales: 980, shopName: '数码旗舰店' },
  { rank: 9, productId: 27, productName: '三合一冲锋衣', productImage: '', minPrice: 59900, sales: 870, shopName: '户外探险家' },
  { rank: 10, productId: 16, productName: '乐高城堡积木', productImage: '', minPrice: 49900, sales: 760, shopName: '母婴优选' },
]

// 统计数据
function getStatistics() {
  const pendingCount = merchants.filter(m => m.status === 0).length
  return {
    totalUsers: users.length + merchants.length + admins.length,
    totalMerchants: merchants.filter(m => m.status === 1).length,
    pendingMerchants: pendingCount,
    totalOrders: 486,
    totalSales: 2589600,
  }
}

// -------- API 模拟 --------

export function mockAdminLogin(username, password) {
  const admin = admins.find(u => u.username === username && u.password === password)
  if (admin) return { ...admin, password: undefined }
  // 也允许普通用户尝试（但会被 auth store 拦截）
  const user = [...users, ...merchants].find(u => u.username === username)
  if (user && user.password === password) return { ...user, password: undefined }
  return null
}

export function mockGetStatistics() {
  return getStatistics()
}

export function mockGetPendingMerchants(page = 1, size = 20) {
  const pending = merchants.filter(m => m.status === 0)
  const start = (page - 1) * size
  return {
    records: pending.slice(start, start + size),
    total: pending.length,
    page,
    size,
  }
}

export function mockGetAllUsers(page = 1, size = 20, role = null) {
  let all = [...users]
  if (role !== null && role !== '') {
    all = all.filter(u => u.role === Number(role))
  }
  const start = (page - 1) * size
  return {
    records: all.slice(start, start + size),
    total: all.length,
    page,
    size,
  }
}

export function mockApproveMerchant(userId, approved) {
  const m = merchants.find(u => u.id === userId)
  if (!m) return false
  if (approved) {
    m.status = 1
  } else {
    const idx = merchants.indexOf(m)
    if (idx > -1) merchants.splice(idx, 1)
  }
  return true
}

export function mockToggleUserStatus(userId, status) {
  const u = [...users, ...merchants].find(u => u.id === userId)
  if (!u) return false
  u.status = status
  return true
}

export function mockGetRanking(limit = 10) {
  return rankings.slice(0, limit)
}

// 订单数据
const orders = [
  { id: 1, orderNo: '2072677744226996224', userId: 2, shopId: 1, totalAmount: 89900, discountAmount: 0, status: 0, addressId: 1, receiverName: '张三', receiverPhone: '138****0001', receiverAddr: '广东省广州市番禺区...', remark: '', payNo: null, payTime: null, cancelTime: null, createTime: '2026-07-02 21:30:00', items: [{ id: 1, productId: 1, productName: 'iPhone 15 Pro Max', skuName: '颜色:黑; 存储:256G', productImage: '', price: 89900, quantity: 1 }] },
  { id: 2, orderNo: '2072677744226996225', userId: 2, shopId: 4, totalAmount: 12800, discountAmount: 500, status: 1, addressId: 1, receiverName: '张三', receiverPhone: '138****0001', receiverAddr: '广东省广州市番禺区...', remark: '', payNo: '2026070222001', payTime: '2026-07-01 15:20:00', cancelTime: null, createTime: '2026-07-01 15:10:00', items: [{ id: 2, productId: 18, productName: '三只松鼠坚果礼盒', skuName: '', productImage: '', price: 12800, quantity: 1 }] },
  { id: 3, orderNo: '2072677744226996226', userId: 2, shopId: 2, totalAmount: 12900, discountAmount: 0, status: 2, addressId: 1, receiverName: '张三', receiverPhone: '138****0001', receiverAddr: '广东省广州市番禺区...', remark: '', payNo: '2026070122002', payTime: '2026-06-30 10:00:00', cancelTime: null, createTime: '2026-06-30 09:45:00', items: [{ id: 3, productId: 10, productName: '韩版宽松卫衣', skuName: '颜色:白; 尺码:L', productImage: '', price: 12900, quantity: 1 }] },
  { id: 4, orderNo: '2072677744226996227', userId: 3, shopId: 1, totalAmount: 249900, discountAmount: 10000, status: 3, addressId: 2, receiverName: '李四', receiverPhone: '139****0002', receiverAddr: '北京市朝阳区xxx路...', remark: '请尽快发货', payNo: '2026062522003', payTime: '2026-06-25 14:00:00', cancelTime: null, createTime: '2026-06-25 13:50:00', items: [{ id: 4, productId: 4, productName: 'Sony WH-1000XM5', skuName: '颜色:银', productImage: '', price: 249900, quantity: 1 }] },
  { id: 5, orderNo: '2072677744226996228', userId: 3, shopId: 5, totalAmount: 59900, discountAmount: 0, status: 4, addressId: 2, receiverName: '李四', receiverPhone: '139****0002', receiverAddr: '北京市朝阳区xxx路...', remark: '', payNo: null, payTime: null, cancelTime: '2026-06-24 12:00:00', createTime: '2026-06-24 11:30:00', items: [{ id: 5, productId: 27, productName: '三合一冲锋衣', skuName: '尺码:XL', productImage: '', price: 59900, quantity: 1 }] },
  { id: 6, orderNo: '2072677744226996229', userId: 2, shopId: 3, totalAmount: 89900, discountAmount: 0, status: 5, addressId: 1, receiverName: '张三', receiverPhone: '138****0001', receiverAddr: '广东省广州市番禺区...', remark: '', payNo: '2026062022004', payTime: '2026-06-20 09:00:00', cancelTime: null, createTime: '2026-06-20 08:45:00', items: [{ id: 6, productId: 13, productName: '兰蔻精华肌底液', skuName: '容量:50ml', productImage: '', price: 89900, quantity: 1 }] },
]

export function mockGetMyOrders(page = 1, size = 20, status = null) {
  let list = orders
  if (status !== null && status !== '' && status !== undefined) {
    list = list.filter(o => o.status === Number(status))
  }
  const start = (page - 1) * size
  return {
    records: list.slice(start, start + size).map(o => ({ ...o, statusText: statusLabel(o.status), payAmount: o.totalAmount - o.discountAmount, items: undefined })),
    total: list.length,
    page,
    size,
  }
}

export function mockGetOrderDetail(orderNo) {
  const o = orders.find(o => o.orderNo === orderNo)
  if (!o) return null
  return {
    ...o,
    statusText: statusLabel(o.status),
    payAmount: o.totalAmount - o.discountAmount,
    receiverFullName: o.receiverName,
    receiverFullPhone: o.receiverPhone,
    receiverFullAddr: o.receiverAddr,
  }
}

export function mockCancelOrder(orderNo) {
  const o = orders.find(o => o.orderNo === orderNo)
  if (!o) return false
  o.status = 4
  o.cancelTime = new Date().toISOString().replace('T', ' ').slice(0, 19)
  return true
}

export function mockUpdateOrderStatus(orderNo, status) {
  const o = orders.find(o => o.orderNo === orderNo)
  if (!o) return false
  o.status = status
  return true
}

function statusLabel(status) {
  return ['待支付', '已支付', '已发货', '已完成', '已取消', '已退款'][status] || '未知'
}
