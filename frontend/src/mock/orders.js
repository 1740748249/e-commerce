let orderId = 1

export const orders = [
  { id: orderId++, productId: 1, productName: 'iPhone 15 Pro Max', price: 9999, qty: 1, total: 9999, buyerId: 1, buyerName: '张三', buyerPhone: '13800001001', buyerAddr: '北京市朝阳区xxx路1号', shopId: 1, status: '待支付', time: '2026-06-14 10:30' },
  { id: orderId++, productId: 3, productName: 'Nike Air Jordan 1', price: 1299, qty: 2, total: 2598, buyerId: 1, buyerName: '张三', buyerPhone: '13800001001', buyerAddr: '北京市朝阳区xxx路1号', shopId: 2, status: '已发货', time: '2026-06-13 15:20' },
  { id: orderId++, productId: 5, productName: '华为 Mate 60 Pro', price: 6999, qty: 1, total: 6999, buyerId: 2, buyerName: '李四', buyerPhone: '13900002002', buyerAddr: '上海市浦东新区yyy路2号', shopId: 3, status: '退款中', time: '2026-06-14 09:15' },
]

export function createOrder(orderData) {
  const o = { ...orderData, id: orderId++, time: new Date().toLocaleString(), status: '待支付' }
  orders.unshift(o)
  return o
}

export function updateOrderStatus(id, status) {
  const o = orders.find(o => o.id === id)
  if (o) o.status = status
  return o
}
