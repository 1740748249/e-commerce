let notifId = 1

export const notifications = [
  { id: notifId++, shopId: 1, type: 'order', title: '新订单', content: '张三 下单了 iPhone 15 Pro Max，金额 ¥9999', orderId: 1, read: false, time: '2026-06-14 10:30' },
  { id: notifId++, shopId: 2, type: 'order', title: '新订单', content: '张三 下单了 Nike Air Jordan 1 ×2，金额 ¥2598', orderId: 2, read: false, time: '2026-06-13 15:20' },
  { id: notifId++, shopId: 3, type: 'order', title: '新订单', content: '李四 下单了 华为 Mate 60 Pro，金额 ¥6999', orderId: 3, read: true, time: '2026-06-14 09:15' },
]

export function addNotification(notif) {
  const n = { ...notif, id: notifId++, time: new Date().toLocaleString(), read: false }
  notifications.unshift(n)
  return n
}

export function getShopNotifications(shopId) {
  return notifications.filter(n => n.shopId === shopId)
}
