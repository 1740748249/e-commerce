export const categories = ['全部', '数码', '运动', '家电', '食品', '玩具']

export const shops = [
  { id: 1, name: '苹果旗舰店', status: 1 },
  { id: 2, name: '耐克官方店', status: 1 },
  { id: 3, name: '华为官方旗舰', status: 1 },
  { id: 4, name: '阿迪达斯旗舰', status: 1 },
  { id: 5, name: '数码好物店', status: 1 },
  { id: 6, name: '家电直销', status: 1 },
  { id: 7, name: '名酒坊', status: 1 },
  { id: 8, name: '零食铺子', status: 1 },
  { id: 9, name: '乐高旗舰', status: 1 },
]

let skuIdCounter = 1000
function nextSkuId() { return skuIdCounter++ }

function defaultSku(product, price, stock) {
  return [{ id: nextSkuId(), sku_code: 'DEFAULT-' + product.id, specs: { '默认': '默认' }, price: price || product.price, stock: stock || product.stock, image: product.image }]
}

const rawProducts = [
  {
    id: 1, name: 'iPhone 15 Pro Max', category: '数码', price: 8999, stock: 50, shopId: 1, shopName: '苹果旗舰店',
    image: 'https://picsum.photos/seed/p1/400/400', desc: 'A17 Pro 芯片，钛金属设计，4800 万像素主摄',
    specGroups: [
      { name: '颜色', options: ['黑色', '白色', '原色钛金属'] },
      { name: '存储', options: ['256G', '512G', '1TB'] }
    ],
    _skuMatrix: {
      '黑色-256G':   { price: 8999,  stock: 20, image: 'https://picsum.photos/seed/p1b/400/400' },
      '黑色-512G':   { price: 9999,  stock: 15, image: 'https://picsum.photos/seed/p1b/400/400' },
      '黑色-1TB':    { price: 11999, stock: 5,  image: 'https://picsum.photos/seed/p1b/400/400' },
      '白色-256G':   { price: 8999,  stock: 18, image: 'https://picsum.photos/seed/p1w/400/400' },
      '白色-512G':   { price: 9999,  stock: 10, image: 'https://picsum.photos/seed/p1w/400/400' },
      '白色-1TB':    { price: 11999, stock: 3,  image: 'https://picsum.photos/seed/p1w/400/400' },
      '原色钛金属-256G': { price: 8999, stock: 22, image: 'https://picsum.photos/seed/p1n/400/400' },
      '原色钛金属-512G': { price: 9999, stock: 12, image: 'https://picsum.photos/seed/p1n/400/400' },
      '原色钛金属-1TB':  { price: 11999, stock: 4, image: 'https://picsum.photos/seed/p1n/400/400' },
    }
  },
  {
    id: 2, name: 'MacBook Air 15', category: '数码', price: 10499, stock: 30, shopId: 1, shopName: '苹果旗舰店',
    image: 'https://picsum.photos/seed/p2/400/400', desc: 'M3 芯片，15.3 英寸 Liquid Retina 显示屏',
    specGroups: [
      { name: '颜色', options: ['午夜色', '星光色', '深空灰'] },
      { name: '内存', options: ['8GB', '16GB'] },
      { name: '存储', options: ['256G', '512G'] }
    ],
    _skuMatrix: {
      '午夜色-8GB-256G':  { price: 10499, stock: 10, image: 'https://picsum.photos/seed/p2m/400/400' },
      '午夜色-8GB-512G':  { price: 11999, stock: 5,  image: 'https://picsum.photos/seed/p2m/400/400' },
      '午夜色-16GB-256G': { price: 12499, stock: 3,  image: 'https://picsum.photos/seed/p2m/400/400' },
      '午夜色-16GB-512G': { price: 13999, stock: 2,  image: 'https://picsum.photos/seed/p2m/400/400' },
      '星光色-8GB-256G':  { price: 10499, stock: 8,  image: 'https://picsum.photos/seed/p2s/400/400' },
      '星光色-8GB-512G':  { price: 11999, stock: 4,  image: 'https://picsum.photos/seed/p2s/400/400' },
      '星光色-16GB-256G': { price: 12499, stock: 2,  image: 'https://picsum.photos/seed/p2s/400/400' },
      '星光色-16GB-512G': { price: 13999, stock: 1,  image: 'https://picsum.photos/seed/p2s/400/400' },
      '深空灰-8GB-256G':  { price: 10499, stock: 6,  image: 'https://picsum.photos/seed/p2g/400/400' },
      '深空灰-8GB-512G':  { price: 11999, stock: 3,  image: 'https://picsum.photos/seed/p2g/400/400' },
      '深空灰-16GB-256G': { price: 12499, stock: 2,  image: 'https://picsum.photos/seed/p2g/400/400' },
      '深空灰-16GB-512G': { price: 13999, stock: 1,  image: 'https://picsum.photos/seed/p2g/400/400' },
    }
  },
  {
    id: 3, name: 'Nike Air Jordan 1', category: '运动', price: 1299, stock: 100, shopId: 2, shopName: '耐克官方店',
    image: 'https://picsum.photos/seed/p3/400/400', desc: '经典复刻，全粒面皮革鞋面',
    specGroups: [
      { name: '颜色', options: ['黑白', '红黑', '纯白'] },
      { name: '尺码', options: ['39', '40', '41', '42', '43'] }
    ],
    _skuMatrix: {
      '黑白-39': { price: 1299, stock: 10, image: 'https://picsum.photos/seed/p3bw/400/400' },
      '黑白-40': { price: 1299, stock: 12, image: 'https://picsum.photos/seed/p3bw/400/400' },
      '黑白-41': { price: 1299, stock: 15, image: 'https://picsum.photos/seed/p3bw/400/400' },
      '黑白-42': { price: 1299, stock: 8,  image: 'https://picsum.photos/seed/p3bw/400/400' },
      '黑白-43': { price: 1299, stock: 5,  image: 'https://picsum.photos/seed/p3bw/400/400' },
      '红黑-39': { price: 1399, stock: 6,  image: 'https://picsum.photos/seed/p3rb/400/400' },
      '红黑-40': { price: 1399, stock: 8,  image: 'https://picsum.photos/seed/p3rb/400/400' },
      '红黑-41': { price: 1399, stock: 10, image: 'https://picsum.photos/seed/p3rb/400/400' },
      '红黑-42': { price: 1399, stock: 4,  image: 'https://picsum.photos/seed/p3rb/400/400' },
      '红黑-43': { price: 1399, stock: 2,  image: 'https://picsum.photos/seed/p3rb/400/400' },
      '纯白-39': { price: 1199, stock: 8,  image: 'https://picsum.photos/seed/p3w/400/400' },
      '纯白-40': { price: 1199, stock: 10, image: 'https://picsum.photos/seed/p3w/400/400' },
      '纯白-41': { price: 1199, stock: 12, image: 'https://picsum.photos/seed/p3w/400/400' },
      '纯白-42': { price: 1199, stock: 6,  image: 'https://picsum.photos/seed/p3w/400/400' },
      '纯白-43': { price: 1199, stock: 4,  image: 'https://picsum.photos/seed/p3w/400/400' },
    }
  },
  { id: 4, name: 'Nike Dunk Low', category: '运动', price: 899, stock: 80, shopId: 2, shopName: '耐克官方店', image: 'https://picsum.photos/seed/p4/400/400', desc: '复古篮球鞋，耐磨橡胶外底' },
  {
    id: 5, name: '华为 Mate 60 Pro', category: '数码', price: 6999, stock: 20, shopId: 3, shopName: '华为官方旗舰',
    image: 'https://picsum.photos/seed/p5/400/400', desc: '麒麟 9000S，卫星通信，XMAGE 影像',
    specGroups: [
      { name: '颜色', options: ['雅丹黑', '白沙银'] },
      { name: '存储', options: ['256G', '512G', '1TB'] }
    ],
    _skuMatrix: {
      '雅丹黑-256G': { price: 6999, stock: 8,  image: 'https://picsum.photos/seed/p5b/400/400' },
      '雅丹黑-512G': { price: 7999, stock: 5,  image: 'https://picsum.photos/seed/p5b/400/400' },
      '雅丹黑-1TB':  { price: 9499, stock: 2,  image: 'https://picsum.photos/seed/p5b/400/400' },
      '白沙银-256G': { price: 6999, stock: 6,  image: 'https://picsum.photos/seed/p5w/400/400' },
      '白沙银-512G': { price: 7999, stock: 3,  image: 'https://picsum.photos/seed/p5w/400/400' },
      '白沙银-1TB':  { price: 9499, stock: 1,  image: 'https://picsum.photos/seed/p5w/400/400' },
    }
  },
  { id: 6, name: 'Adidas Ultraboost', category: '运动', price: 1099, stock: 60, shopId: 4, shopName: '阿迪达斯旗舰', image: 'https://picsum.photos/seed/p6/400/400', desc: 'Boost 中底科技，Primeknit 编织鞋面' },
  { id: 7, name: 'Sony WH-1000XM5', category: '数码', price: 2499, stock: 40, shopId: 5, shopName: '数码好物店', image: 'https://picsum.photos/seed/p7/400/400', desc: '行业领先降噪，30 小时续航' },
  { id: 8, name: '戴森 V15 吸尘器', category: '家电', price: 4990, stock: 15, shopId: 6, shopName: '家电直销', image: 'https://picsum.photos/seed/p8/400/400', desc: '激光探测微尘，Dyson Hyperdymium 马达' },
  { id: 9, name: '茅台飞天 53度', category: '食品', price: 2899, stock: 5, shopId: 7, shopName: '名酒坊', image: 'https://picsum.photos/seed/p9/400/400', desc: '酱香型白酒，500ml，国宴品质' },
  { id: 10, name: '三只松鼠坚果礼盒', category: '食品', price: 168, stock: 200, shopId: 8, shopName: '零食铺子', image: 'https://picsum.photos/seed/p10/400/400', desc: '每日坚果混合装 750g' },
  { id: 11, name: 'LEGO 兰博基尼', category: '玩具', price: 2999, stock: 10, shopId: 9, shopName: '乐高旗舰', image: 'https://picsum.photos/seed/p11/400/400', desc: '机械组 42115，3696 颗粒' },
  { id: 12, name: 'iPad Pro M4', category: '数码', price: 8499, stock: 25, shopId: 1, shopName: '苹果旗舰店', image: 'https://picsum.photos/seed/p12/400/400', desc: 'M4 芯片，Ultra Retina XDR 显示屏' },
  { id: 13, name: 'AirPods Pro 2', category: '数码', price: 1899, stock: 60, shopId: 1, shopName: '苹果旗舰店', image: 'https://picsum.photos/seed/p13/400/400', desc: '自适应降噪，MagSafe 充电盒' },
  { id: 14, name: 'Switch OLED', category: '玩具', price: 2349, stock: 30, shopId: 9, shopName: '乐高旗舰', image: 'https://picsum.photos/seed/p14/400/400', desc: '7英寸OLED屏，64GB存储' },
  { id: 15, name: 'Samsung Galaxy S24', category: '数码', price: 5999, stock: 35, shopId: 5, shopName: '数码好物店', image: 'https://picsum.photos/seed/p15/400/400', desc: 'Galaxy AI，钛金属边框' },
  { id: 16, name: 'Adidas Yeezy 350', category: '运动', price: 1599, stock: 40, shopId: 4, shopName: '阿迪达斯旗舰', image: 'https://picsum.photos/seed/p16/400/400', desc: 'Primeknit 鞋面，Boost 缓震' },
  { id: 17, name: '小米电视 S75', category: '家电', price: 3499, stock: 20, shopId: 6, shopName: '家电直销', image: 'https://picsum.photos/seed/p17/400/400', desc: '75英寸4K，144Hz高刷' },
  { id: 18, name: '良品铺子零食大礼包', category: '食品', price: 128, stock: 300, shopId: 8, shopName: '零食铺子', image: 'https://picsum.photos/seed/p18/400/400', desc: '12袋组合装，办公室必备' },
  { id: 19, name: 'Apple Watch Ultra 2', category: '数码', price: 5999, stock: 15, shopId: 1, shopName: '苹果旗舰店', image: 'https://picsum.photos/seed/p19/400/400', desc: '钛金属表壳，双频GPS' },
  { id: 20, name: 'New Balance 574', category: '运动', price: 699, stock: 120, shopId: 2, shopName: '耐克官方店', image: 'https://picsum.photos/seed/p20/400/400', desc: '经典复古跑鞋，ENCAP缓震' },
  { id: 21, name: '戴森 HD15 吹风机', category: '家电', price: 3199, stock: 25, shopId: 6, shopName: '家电直销', image: 'https://picsum.photos/seed/p21/400/400', desc: '智能温控，快速干发不伤发' },
  { id: 22, name: '五粮液 52度', category: '食品', price: 1099, stock: 30, shopId: 7, shopName: '名酒坊', image: 'https://picsum.photos/seed/p22/400/400', desc: '浓香型白酒，500ml经典装' },
  { id: 23, name: '大疆 Mini 4 Pro', category: '数码', price: 5788, stock: 10, shopId: 5, shopName: '数码好物店', image: 'https://picsum.photos/seed/p23/400/400', desc: '4K/60fps HDR，全向避障' },
  { id: 24, name: 'LEGO 哈利波特城堡', category: '玩具', price: 3499, stock: 8, shopId: 9, shopName: '乐高旗舰', image: 'https://picsum.photos/seed/p24/400/400', desc: '霍格沃茨城堡，6020颗粒' },
  { id: 25, name: '华为 MatePad Pro', category: '数码', price: 3299, stock: 20, shopId: 3, shopName: '华为官方旗舰', image: 'https://picsum.photos/seed/p25/400/400', desc: '13.2英寸OLED，星闪连接' },
  { id: 26, name: '李宁飞电3', category: '运动', price: 899, stock: 90, shopId: 2, shopName: '耐克官方店', image: 'https://picsum.photos/seed/p26/400/400', desc: '碳板跑鞋，䨻科技中底' },
  { id: 27, name: '松下纳米水离子吹风机', category: '家电', price: 699, stock: 45, shopId: 6, shopName: '家电直销', image: 'https://picsum.photos/seed/p27/400/400', desc: '纳米水离子技术，润泽护发' },
  { id: 28, name: '百草味每日坚果', category: '食品', price: 89, stock: 500, shopId: 8, shopName: '零食铺子', image: 'https://picsum.photos/seed/p28/400/400', desc: '30日装，混合坚果750g' },
  { id: 29, name: '索尼 PS5 Slim', category: '玩具', price: 3499, stock: 15, shopId: 5, shopName: '数码好物店', image: 'https://picsum.photos/seed/p29/400/400', desc: '轻薄设计，1TB固态硬盘' },
  { id: 30, name: 'LG 27GP950', category: '数码', price: 4299, stock: 12, shopId: 5, shopName: '数码好物店', image: 'https://picsum.photos/seed/p30/400/400', desc: '27英寸4K 144Hz，Nano IPS' },
]

// Build products with real SKU arrays from _skuMatrix, or default SKUs
export const products = rawProducts.map(p => {
  if (p._skuMatrix && p.specGroups) {
    p.skus = p.specGroups.reduce((acc, group) => {
      if (acc.length === 0) return group.options.map(opt => [opt])
      return acc.flatMap(combo => group.options.map(opt => [...combo, opt]))
    }, []).map(combo => {
      const key = combo.join('-')
      const data = p._skuMatrix[key] || { price: p.price, stock: 0 }
      const specs = {}
      p.specGroups.forEach((g, i) => { specs[g.name] = combo[i] })
      return { id: nextSkuId(), product_id: p.id, sku_code: 'SKU-' + p.id + '-' + combo.join('-'), specs, price: data.price, stock: data.stock, image: data.image }
    })
    delete p._skuMatrix
    p.price = Math.min(...p.skus.map(s => s.price))
    p.stock = p.skus.reduce((sum, s) => sum + s.stock, 0)
  } else {
    p.skus = defaultSku(p, p.price, p.stock)
  }
  return p
})
