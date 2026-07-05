export const users = [
  { id: 1, username: 'zhangsan', password: '123456', name: '张三', phone: '13800001001', addr: '北京市朝阳区xxx路1号', role: 'user' },
  { id: 2, username: 'lisi', password: '123456', name: '李四', phone: '13900002002', addr: '上海市浦东新区yyy路2号', role: 'user' },
  { id: 3, username: 'admin_apple', password: '123456', name: '苹果旗舰店', phone: '13700003003', addr: '深圳市南山区zzz路3号', role: 'vendor', shopId: 1, shopName: '苹果旗舰店' },
  { id: 4, username: 'admin_nike', password: '123456', name: '耐克官方店', phone: '13600004004', addr: '广州市天河区aaa路4号', role: 'vendor', shopId: 2, shopName: '耐克官方店' },
]

export function findUser(username, password) {
  return users.find(u => u.username === username && u.password === password) || null
}

export function registerUser(user) {
  const u = { id: users.length + 1, ...user, role: 'user' }
  users.push(u)
  return u
}
