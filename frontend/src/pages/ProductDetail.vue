<template>
  <div v-if="p" class="product-detail">
    <!-- ==================== PC 布局 ==================== -->
    <div class="pc-layout">
      <!-- 面包屑 -->
      <nav class="breadcrumb">
        <router-link to="/">首页</router-link>
        <span class="sep">/</span>
        <router-link :to="`/shop/${p.shopId}`">{{ p.shopName }}</router-link>
        <span class="sep">/</span>
        <span class="current">{{ p.name }}</span>
      </nav>

      <div class="pc-main">
        <!-- 左侧：图片画廊 -->
        <div class="gallery">
          <div class="main-image-wrap">
            <img :src="currentImage" :alt="p.name" class="main-image" />
            <div v-if="p.sales > 100" class="hot-badge">🔥 热销 {{ p.sales }}+</div>
          </div>
          <div v-if="p.skus && p.skus.length > 1" class="thumbnail-list">
            <button
              v-for="(sku, i) in p.skus.slice(0, 6)"
              :key="i"
              :class="['thumb', { active: currentImage === (sku.image || p.image) }]"
              @click="selectSkuImage(sku)"
            >
              <img :src="sku.image || p.image" />
            </button>
          </div>
        </div>

        <!-- 右侧：商品信息 -->
        <div class="info">
          <!-- 店铺标签 -->
          <router-link :to="`/shop/${p.shopId}`" class="shop-tag">
            <span class="shop-avatar">{{ p.shopName?.[0] }}</span>
            <span>{{ p.shopName }}</span>
            <span class="arrow">›</span>
          </router-link>

          <h1 class="product-name">{{ p.name }}</h1>
          <p class="product-desc">{{ p.desc }}</p>

          <!-- 价格区 -->
          <div class="price-box">
            <div class="price-main">
              <span class="currency">¥</span>
              <span class="value">{{ (currentPrice / 100).toFixed(2) }}</span>
            </div>
            <div v-if="currentStock > 0 && currentStock <= 10" class="stock-tag low">库存紧张 · 仅剩 {{ currentStock }} 件</div>
            <div v-else-if="currentStock > 0" class="stock-tag ok">有货 · 库存 {{ currentStock }} 件</div>
            <div v-else class="stock-tag none">暂时缺货</div>
          </div>

          <!-- 服务保障 -->
          <div class="service-row">
            <div class="service-item"><span class="icon">✓</span> 包邮</div>
            <div class="service-item"><span class="icon">✓</span> 7天无理由</div>
            <div class="service-item"><span class="icon">✓</span> 正品保障</div>
            <div class="service-item"><span class="icon">✓</span> 极速发货</div>
          </div>

          <!-- 规格选择 -->
          <div v-if="p.specGroups && p.specGroups.length" class="spec-section">
            <div v-for="group in p.specGroups" :key="group.name" class="spec-group">
              <label class="spec-label">{{ group.name }}</label>
              <div class="spec-options">
                <button
                  v-for="opt in group.options"
                  :key="opt"
                  :class="['spec-btn', { active: selectedSpecs[group.name] === opt }]"
                  @click="selectSpec(group.name, opt)"
                >{{ opt }}</button>
              </div>
            </div>
          </div>

          <!-- 数量选择 + 操作 -->
          <div class="action-row">
            <div class="qty-control">
              <button :disabled="qty <= 1" @click="qty--">−</button>
              <input type="number" v-model.number="qty" :min="1" :max="maxQty" />
              <button :disabled="qty >= maxQty" @click="qty++">+</button>
            </div>
            <button :disabled="!canBuy" class="btn-cart" @click="doAddCart">加入购物车</button>
            <button :disabled="!canBuy" class="btn-buy" @click="buyNow">立即购买</button>
          </div>

          <!-- 已加购提示 -->
          <Transition name="fade">
            <p v-if="success" class="success-msg">✓ 已成功加入购物车</p>
          </Transition>

          <!-- 收藏 / 分享 -->
          <div class="extra-actions">
            <button class="extra-btn">♡ 收藏</button>
            <button class="extra-btn">↗ 分享</button>
          </div>
        </div>
      </div>

      <!-- PC 底部：商品详情描述 -->
      <div class="pc-detail-section">
        <h3 class="section-title">商品详情</h3>
        <div class="detail-content">
          <p>{{ p.desc }}</p>
          <div class="detail-placeholder">
            <div class="placeholder-icon">📦</div>
            <p>详细图文描述请查看后端商品详情接口</p>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 移动端布局 ==================== -->
    <div class="mobile-layout">
      <div class="mobile-gallery-wrap">
        <img :src="currentImage" class="mobile-hero" />
        <button @click="$router.back()" class="mobile-back">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" /></svg>
        </button>
      </div>

      <div class="mobile-card">
        <div class="mobile-price-row">
          <div class="mobile-price">
            <span class="mobile-currency">¥</span>
            <span class="mobile-value">{{ (currentPrice / 100).toFixed(0) }}</span>
          </div>
          <span class="mobile-badge">特惠</span>
        </div>
        <div class="mobile-meta">
          <span>库存 {{ currentStock }} 件</span>
          <span>已售 {{ p.sales || 0 }}</span>
        </div>

        <!-- 单规格：直接展示选择 -->
        <div v-if="p.specGroups && p.specGroups.length === 1" class="mobile-single-spec">
          <p class="mobile-spec-hint">已选：<strong>{{ selectedSpecs[p.specGroups[0].name] }}</strong></p>
          <div class="mobile-spec-options">
            <button
              v-for="opt in p.specGroups[0].options"
              :key="opt"
              :class="['mobile-spec-btn', { active: selectedSpecs[p.specGroups[0].name] === opt }]"
              @click="selectSpec(p.specGroups[0].name, opt)"
            >{{ opt }}</button>
          </div>
        </div>

        <!-- 多规格：弹出层选择 -->
        <div v-if="p.specGroups && p.specGroups.length > 1" class="mobile-multi-spec" @click="openSheet('cart')">
          <span>已选：<strong>{{ specSummary }}</strong></span>
          <span class="mobile-arrow">›</span>
        </div>
      </div>

      <div class="mobile-info-card">
        <h1 class="mobile-name">{{ p.name }}</h1>
        <div class="mobile-service">
          <span>✓ 包邮</span>
          <span>✓ 7天无理由</span>
          <span>✓ 正品保障</span>
        </div>
      </div>

      <router-link :to="`/shop/${p.shopId}`" class="mobile-shop-card">
        <div class="mobile-shop-avatar">{{ p.shopName?.[0] }}</div>
        <div class="mobile-shop-info">
          <p>{{ p.shopName }}</p>
          <span>在线</span>
        </div>
        <span class="mobile-shop-arrow">进店 ›</span>
      </router-link>

      <div class="mobile-detail-card">
        <h3>商品详情</h3>
        <p>{{ p.desc }}</p>
      </div>

      <div class="mobile-spacer"></div>
    </div>

    <!-- 移动端底部操作栏 -->
    <div class="mobile-bar">
      <router-link to="/cart" class="mobile-bar-cart">
        <span>🛒</span>
        <i v-if="cart.count" class="cart-dot">{{ cart.count }}</i>
      </router-link>
      <button class="mobile-bar-btn cart-btn" @click="hasSpecs ? openSheet('cart') : doAddCart()">加入购物车</button>
      <button class="mobile-bar-btn buy-btn" @click="hasSpecs ? openSheet('buy') : buyNow()">立即购买</button>
    </div>

    <!-- 移动端规格弹出层 -->
    <Teleport to="body">
      <Transition name="sheet">
        <div v-if="showSheet" class="sheet-overlay" @click.self="closeSheet">
          <div class="sheet-panel">
            <div class="sheet-header">
              <img :src="currentImage" class="sheet-thumb" />
              <div>
                <p class="sheet-price">¥{{ (currentPrice / 100).toFixed(0) }}</p>
                <p class="sheet-stock">库存 {{ currentStock }} 件 | 已选：{{ specSummary }}</p>
              </div>
              <button class="sheet-close" @click="closeSheet">✕</button>
            </div>
            <div class="sheet-body">
              <div v-for="group in p.specGroups" :key="group.name" class="sheet-spec-group">
                <p class="sheet-spec-label">{{ group.name }}</p>
                <div class="sheet-spec-options">
                  <button
                    v-for="opt in group.options"
                    :key="opt"
                    :class="['sheet-spec-btn', { active: selectedSpecs[group.name] === opt }]"
                    @click="selectSpec(group.name, opt)"
                  >{{ opt }}</button>
                </div>
              </div>
              <div class="sheet-qty-row">
                <span>数量</span>
                <div class="qty-control small">
                  <button :disabled="qty <= 1" @click="qty--">−</button>
                  <span>{{ qty }}</span>
                  <button :disabled="qty >= maxQty" @click="qty++">+</button>
                </div>
              </div>
            </div>
            <div class="sheet-footer">
              <button :disabled="!canBuy" class="sheet-btn cart-btn" @click="doAddCartFromSheet">加入购物车</button>
              <button :disabled="!canBuy" class="sheet-btn buy-btn" @click="buyNowFromSheet">立即购买</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Toast -->
    <Teleport to="body">
      <Transition name="toast">
        <div v-if="success" class="toast-msg">✓ 已加入购物车</div>
      </Transition>
    </Teleport>
  </div>

  <div v-else-if="loading" class="loading-state">加载中...</div>
  <div v-else class="empty-state">
    <div class="empty-icon">😕</div>
    <p>商品不存在</p>
    <router-link to="/" class="empty-link">返回首页</router-link>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProductStore } from '@/stores/product'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useCheckoutStore } from '@/stores/checkout'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()
const cart = useCartStore()
const auth = useAuthStore()
const checkout = useCheckoutStore()

const p = ref(null)
const loading = ref(true)
const qty = ref(1)
const success = ref(false)
const showSheet = ref(false)
const sheetAction = ref('cart')
const selectedSpecs = reactive({})

function initDefaultSpecs() {
  if (p.value?.specGroups) {
    p.value.specGroups.forEach(g => {
      if (!(g.name in selectedSpecs) && g.options.length) {
        selectedSpecs[g.name] = g.options[0]
      }
    })
  }
}

const currentSku = computed(() => {
  if (!p.value?.skus?.length) return null
  if (!p.value.specGroups?.length) return p.value.skus[0]
  const keys = Object.keys(selectedSpecs)
  if (!keys.length) return p.value.skus[0]
  return p.value.skus.find(s => keys.every(k => s.specs[k] === selectedSpecs[k])) || p.value.skus[0]
})
const currentPrice = computed(() => currentSku.value?.price ?? p.value?.price ?? 0)
const currentStock = computed(() => currentSku.value?.stock ?? p.value?.stock ?? 0)
const currentImage = computed(() => currentSku.value?.image || p.value?.image || '')
const maxQty = computed(() => Math.max(1, currentStock.value))
const canBuy = computed(() => currentStock.value > 0)
const hasSpecs = computed(() => p.value?.specGroups?.length > 0)
const specSummary = computed(() => {
  if (!p.value?.specGroups) return '默认'
  return p.value.specGroups.map(g => selectedSpecs[g.name] || g.options[0]).join(' / ')
})

function selectSpec(name, opt) {
  selectedSpecs[name] = opt
  if (qty.value > maxQty.value) qty.value = maxQty.value
}
function selectSkuImage(sku) {
  // find the first spec key that differs for this SKU and select it
  if (!p.value?.specGroups) return
  const keys = Object.keys(sku.specs || {})
  keys.forEach(k => { if (k in selectedSpecs) selectedSpecs[k] = sku.specs[k] })
}
function cartItemData() {
  return {
    productId: p.value.id,
    skuId: currentSku.value?.id || 0,
    skuName: specSummary.value,
    name: p.value.name,
    price: currentPrice.value,
    image: currentImage.value,
    shopId: p.value.shopId,
    shopName: p.value.shopName,
  }
}
async function doAddCart() {
  await cart.add(cartItemData(), qty.value)
  success.value = true
  setTimeout(() => (success.value = false), 1500)
}
function buyNow() {
  if (!auth.isLoggedIn) return router.push('/login')
  goToCheckout()
}
function goToCheckout() {
  checkout.setBuyNowItem(cartItemData())
  router.push('/checkout')
}
function openSheet(action) {
  initDefaultSpecs()
  sheetAction.value = action
  showSheet.value = true
}
function closeSheet() {
  showSheet.value = false
}
async function doAddCartFromSheet() {
  await cart.add(cartItemData(), qty.value)
  closeSheet()
  success.value = true
  setTimeout(() => (success.value = false), 1500)
}
async function buyNowFromSheet() {
  if (!auth.isLoggedIn) {
    closeSheet()
    return router.push('/login')
  }
  closeSheet()
  goToCheckout()
}
watch(() => route.params.id, async id => {
  loading.value = true
  const d = await productStore.fetchDetail(Number(id))
  if (d) { p.value = d; initDefaultSpecs() }
  loading.value = false
})
onMounted(async () => {
  const d = await productStore.fetchDetail(Number(route.params.id))
  if (d) { p.value = d; initDefaultSpecs() }
  loading.value = false
})
</script>

<style scoped>
/* ──── Reset ──── */
input[type='number']::-webkit-outer-spin-button,
input[type='number']::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
input[type='number'] { -moz-appearance: textfield; }

/* ──── Mobile-first hidden/shown ──── */
.pc-layout       { display: none; }
.mobile-layout   { display: block; }
.mobile-bar      { display: flex; }

/* ──── PC Layout ──── */
@media (min-width: 768px) {
  .pc-layout       { display: block; max-width: 1200px; margin: 0 auto; padding: 24px 20px 60px; }
  .mobile-layout   { display: none; }
  .mobile-bar      { display: none; }
}

/* ──── Breadcrumb ──── */
.breadcrumb {
  display: flex; align-items: center; gap: 8px; font-size: 13px;
  color: #94a3b8; margin-bottom: 20px;
}
.breadcrumb a { color: #64748b; text-decoration: none; transition: color .2s; }
.breadcrumb a:hover { color: #f97316; }
.breadcrumb .sep { color: #cbd5e1; }
.breadcrumb .current { color: #1e293b; font-weight: 500; }

/* ──── Gallery ──── */
.pc-main { display: flex; gap: 48px; }
.gallery { flex: 0 0 480px; }
.main-image-wrap {
  position: relative; border-radius: 16px; overflow: hidden;
  background: #f8fafc; aspect-ratio: 1;
}
.main-image { width: 100%; height: 100%; object-fit: cover; transition: transform .4s; }
.main-image-wrap:hover .main-image { transform: scale(1.03); }
.hot-badge {
  position: absolute; top: 12px; left: 12px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: #fff; font-size: 12px; font-weight: 600;
  padding: 4px 12px; border-radius: 20px; letter-spacing: 0.5px;
}
.thumbnail-list { display: flex; gap: 10px; margin-top: 12px; }
.thumb {
  width: 64px; height: 64px; border-radius: 10px; overflow: hidden;
  border: 2px solid transparent; cursor: pointer; padding: 0; background: #f1f5f9;
  transition: border-color .2s, box-shadow .2s;
}
.thumb.active { border-color: #f97316; box-shadow: 0 0 0 2px rgba(249,115,22,.15); }
.thumb img { width: 100%; height: 100%; object-fit: cover; }

/* ──── Info ──── */
.info { flex: 1; min-width: 0; }
.shop-tag {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 13px; color: #64748b; text-decoration: none;
  background: #f8fafc; padding: 4px 12px 4px 4px; border-radius: 20px;
  transition: background .2s; margin-bottom: 12px;
}
.shop-tag:hover { background: #fef3c7; }
.shop-avatar {
  width: 26px; height: 26px; border-radius: 50%;
  background: linear-gradient(135deg, #f97316, #f59e0b);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700;
}
.shop-tag .arrow { margin-left: 2px; color: #94a3b8; }

.product-name { font-size: 26px; font-weight: 700; color: #0f172a; line-height: 1.3; margin: 0 0 8px; }
.product-desc { font-size: 14px; color: #64748b; line-height: 1.6; margin: 0 0 20px; }

/* ──── Price box ──── */
.price-box {
  background: linear-gradient(135deg, #fff7ed 0%, #fef3c7 100%);
  border-radius: 14px; padding: 20px 24px; margin-bottom: 16px;
}
.price-main { display: flex; align-items: baseline; }
.currency { font-size: 20px; font-weight: 600; color: #ea580c; margin-right: 2px; }
.value { font-size: 38px; font-weight: 800; color: #ea580c; line-height: 1; }
.stock-tag { font-size: 13px; margin-top: 8px; font-weight: 500; }
.stock-tag.low { color: #ef4444; }
.stock-tag.ok { color: #16a34a; }
.stock-tag.none { color: #94a3b8; }

/* ──── Service row ──── */
.service-row { display: flex; gap: 20px; margin-bottom: 20px; flex-wrap: wrap; }
.service-item { font-size: 12px; color: #64748b; display: flex; align-items: center; gap: 4px; }
.service-item .icon { color: #22c55e; font-weight: 700; font-size: 14px; }

/* ──── Specs ──── */
.spec-section { border-top: 1px solid #f1f5f9; padding-top: 20px; margin-bottom: 20px; }
.spec-group { margin-bottom: 16px; }
.spec-label { display: block; font-size: 13px; font-weight: 600; color: #334155; margin-bottom: 8px; }
.spec-options { display: flex; gap: 8px; flex-wrap: wrap; }
.spec-btn {
  padding: 8px 18px; border-radius: 8px; border: 1.5px solid #e2e8f0;
  background: #fff; font-size: 13px; color: #475569; cursor: pointer;
  transition: all .2s; font-weight: 500;
}
.spec-btn:hover { border-color: #f97316; color: #f97316; background: #fff7ed; }
.spec-btn.active {
  border-color: #f97316; background: linear-gradient(135deg, #fff7ed, #fef3c7);
  color: #ea580c; font-weight: 600; box-shadow: 0 1px 3px rgba(249,115,22,.12);
}

/* ──── Action row ──── */
.action-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.qty-control {
  display: flex; align-items: center; border: 1.5px solid #e2e8f0; border-radius: 10px; overflow: hidden;
}
.qty-control button {
  width: 36px; height: 42px; border: none; background: #f8fafc;
  font-size: 18px; color: #64748b; cursor: pointer; transition: background .15s;
  display: flex; align-items: center; justify-content: center;
}
.qty-control button:hover:not(:disabled) { background: #f1f5f9; color: #1e293b; }
.qty-control button:disabled { opacity: 0.3; cursor: not-allowed; }
.qty-control input {
  width: 52px; height: 42px; border: none; border-left: 1px solid #e2e8f0; border-right: 1px solid #e2e8f0;
  text-align: center; font-size: 15px; font-weight: 600; color: #1e293b; background: #fff;
}
.qty-control.small button { width: 30px; height: 34px; font-size: 16px; }
.qty-control.small input { width: 44px; height: 34px; font-size: 14px; }
.qty-control.small span { width: 44px; text-align: center; font-weight: 600; font-size: 14px; }
.btn-cart {
  flex: 1; height: 44px; border-radius: 10px; border: 2px solid #f97316;
  background: #fff; color: #f97316; font-size: 15px; font-weight: 600;
  cursor: pointer; transition: all .2s;
}
.btn-cart:hover:not(:disabled) { background: #fff7ed; box-shadow: 0 2px 8px rgba(249,115,22,.12); }
.btn-buy {
  flex: 1; height: 44px; border-radius: 10px; border: none;
  background: linear-gradient(135deg, #f97316, #ea580c);
  color: #fff; font-size: 15px; font-weight: 600;
  cursor: pointer; transition: all .2s; box-shadow: 0 2px 12px rgba(249,115,22,.25);
}
.btn-buy:hover:not(:disabled) {
  transform: translateY(-1px); box-shadow: 0 4px 16px rgba(249,115,22,.35);
}
.btn-cart:disabled, .btn-buy:disabled { opacity: 0.45; cursor: not-allowed; }

.success-msg { color: #16a34a; font-size: 14px; font-weight: 500; margin-top: 8px; }

/* ──── Extra actions ──── */
.extra-actions { display: flex; gap: 16px; margin-top: 16px; }
.extra-btn {
  background: none; border: none; font-size: 13px; color: #94a3b8;
  cursor: pointer; transition: color .2s; padding: 0;
}
.extra-btn:hover { color: #64748b; }

/* ──── PC detail section ──── */
.pc-detail-section { margin-top: 48px; }
.section-title {
  font-size: 18px; font-weight: 700; color: #0f172a;
  padding-bottom: 12px; border-bottom: 2px solid #f97316; margin-bottom: 20px;
}
.detail-content { color: #475569; font-size: 14px; line-height: 1.8; }
.detail-placeholder {
  margin-top: 32px; text-align: center; padding: 60px 0;
  background: #f8fafc; border-radius: 12px; color: #94a3b8;
}
.placeholder-icon { font-size: 48px; margin-bottom: 12px; }

/* ──── Mobile Layout ──── */
.mobile-layout { background: #f2f3f5; }
.mobile-gallery-wrap { position: relative; }
.mobile-hero { width: 100%; aspect-ratio: 1; object-fit: cover; display: block; }
.mobile-back {
  position: fixed; top: 12px; left: 12px; z-index: 30;
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: rgba(0,0,0,.45); color: #fff;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; backdrop-filter: blur(6px);
}
.mobile-card {
  background: #fff; border-radius: 20px 20px 0 0; margin-top: -24px;
  position: relative; z-index: 10; padding: 20px 16px 16px;
  box-shadow: 0 -4px 20px rgba(0,0,0,.04);
}
.mobile-price-row { display: flex; align-items: flex-end; justify-content: space-between; }
.mobile-currency { font-size: 14px; font-weight: 700; color: #ef4444; }
.mobile-value { font-size: 32px; font-weight: 900; color: #ef4444; line-height: 1; }
.mobile-badge {
  font-size: 11px; color: #fff; background: linear-gradient(135deg, #ef4444, #f97316);
  padding: 3px 10px; border-radius: 12px; font-weight: 600;
}
.mobile-meta { display: flex; gap: 16px; font-size: 12px; color: #94a3b8; margin-top: 6px; }
.mobile-single-spec { margin-top: 12px; }
.mobile-spec-hint { font-size: 12px; color: #64748b; margin-bottom: 6px; }
.mobile-spec-options { display: flex; flex-wrap: wrap; gap: 6px; }
.mobile-spec-btn {
  padding: 6px 14px; border-radius: 6px; border: 1px solid #e2e8f0;
  background: #fff; font-size: 12px; color: #475569; cursor: pointer;
}
.mobile-spec-btn.active { border-color: #ef4444; background: #fef2f2; color: #ef4444; font-weight: 600; }
.mobile-multi-spec {
  margin-top: 12px; display: flex; justify-content: space-between; align-items: center;
  background: #f8fafc; padding: 10px 12px; border-radius: 10px;
  font-size: 12px; color: #64748b; cursor: pointer;
}
.mobile-arrow { color: #94a3b8; font-size: 18px; }

.mobile-info-card {
  background: #fff; margin: 8px 12px 0; border-radius: 16px;
  padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,.02);
}
.mobile-name { font-size: 17px; font-weight: 700; color: #1e293b; line-height: 1.4; margin: 0; }
.mobile-service { display: flex; gap: 12px; margin-top: 10px; font-size: 11px; color: #16a34a; flex-wrap: wrap; }

.mobile-shop-card {
  display: flex; align-items: center; gap: 10px;
  background: #fff; margin: 8px 12px 0; border-radius: 16px;
  padding: 14px 16px; text-decoration: none; box-shadow: 0 1px 3px rgba(0,0,0,.02);
}
.mobile-shop-avatar {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, #f97316, #f59e0b);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; flex-shrink: 0;
}
.mobile-shop-info { flex: 1; }
.mobile-shop-info p { font-size: 14px; font-weight: 600; color: #1e293b; margin: 0; }
.mobile-shop-info span { font-size: 11px; color: #22c55e; }
.mobile-shop-arrow { font-size: 13px; color: #f97316; font-weight: 500; }

.mobile-detail-card {
  background: #fff; margin: 8px 12px 0; border-radius: 16px;
  padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,.02);
}
.mobile-detail-card h3 { font-size: 15px; font-weight: 700; color: #1e293b; margin: 0 0 10px; }
.mobile-detail-card p { font-size: 13px; color: #64748b; line-height: 1.6; margin: 0; }
.mobile-spacer { height: 72px; }

/* ──── Mobile bar ──── */
.mobile-bar {
  position: fixed; bottom: 0; left: 0; right: 0; z-index: 40;
  background: #fff; border-top: 1px solid #f1f5f9;
  padding: 8px 12px; gap: 8px; align-items: center;
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
}
.mobile-bar-cart {
  display: flex; flex-direction: column; align-items: center;
  color: #64748b; text-decoration: none; font-size: 10px; position: relative;
}
.mobile-bar-cart span { font-size: 22px; line-height: 1; }
.cart-dot {
  position: absolute; top: -4px; right: -8px;
  min-width: 16px; height: 16px; border-radius: 8px;
  background: linear-gradient(135deg, #ef4444, #f97316);
  color: #fff; font-size: 10px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  font-style: normal; padding: 0 4px;
}
.mobile-bar-btn {
  flex: 1; height: 40px; border-radius: 20px; border: none;
  font-size: 14px; font-weight: 600; cursor: pointer;
}
.mobile-bar-btn.cart-btn {
  background: linear-gradient(135deg, #f97316, #f59e0b); color: #fff;
}
.mobile-bar-btn.buy-btn {
  background: linear-gradient(135deg, #ef4444, #dc2626); color: #fff;
}
.mobile-bar-btn:disabled { opacity: 0.5; }

/* ──── Sheet overlay ──── */
.sheet-overlay {
  position: fixed; inset: 0; z-index: 50; background: rgba(0,0,0,.45);
  display: flex; align-items: flex-end;
}
.sheet-panel {
  width: 100%; max-height: 85vh; background: #fff;
  border-radius: 20px 20px 0 0; overflow-y: auto;
}
.sheet-header {
  display: flex; gap: 12px; padding: 16px; border-bottom: 1px solid #f1f5f9;
}
.sheet-thumb { width: 80px; height: 80px; border-radius: 12px; object-fit: cover; }
.sheet-price { font-size: 22px; font-weight: 800; color: #ef4444; margin: 4px 0; }
.sheet-stock { font-size: 12px; color: #94a3b8; margin: 0; }
.sheet-close {
  margin-left: auto; align-self: flex-start;
  width: 28px; height: 28px; border-radius: 50%; border: none;
  background: #f1f5f9; font-size: 14px; color: #64748b; cursor: pointer;
}
.sheet-body { padding: 16px; }
.sheet-spec-group { margin-bottom: 16px; }
.sheet-spec-label { font-size: 13px; font-weight: 600; color: #334155; margin: 0 0 8px; }
.sheet-spec-options { display: flex; flex-wrap: wrap; gap: 8px; }
.sheet-spec-btn {
  padding: 8px 16px; border-radius: 8px; border: 1px solid #e2e8f0;
  background: #fff; font-size: 13px; color: #475569; cursor: pointer;
}
.sheet-spec-btn.active { border-color: #ef4444; background: #fef2f2; color: #ef4444; font-weight: 600; }
.sheet-qty-row {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 8px; font-size: 13px; font-weight: 600; color: #334155;
}
.sheet-footer {
  display: flex; gap: 10px; padding: 16px; border-top: 1px solid #f1f5f9;
}
.sheet-btn {
  flex: 1; height: 44px; border-radius: 12px; border: none;
  font-size: 15px; font-weight: 600; cursor: pointer;
}
.sheet-btn.cart-btn { background: linear-gradient(135deg, #f97316, #f59e0b); color: #fff; }
.sheet-btn.buy-btn { background: linear-gradient(135deg, #ef4444, #dc2626); color: #fff; }
.sheet-btn:disabled { opacity: 0.5; }

/* ──── Transitions ──── */
.sheet-enter-active { transition: all .3s ease-out; }
.sheet-leave-active { transition: all .25s ease-in; }
.sheet-enter-from .sheet-panel { transform: translateY(100%); }
.sheet-leave-to .sheet-panel { transform: translateY(100%); }
.sheet-enter-from { opacity: 0; }
.sheet-leave-to { opacity: 0; }
.fade-enter-active { transition: all .3s ease; }
.fade-leave-active { transition: all .3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.toast-msg {
  position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
  background: rgba(0,0,0,.78); color: #fff; padding: 12px 28px;
  border-radius: 10px; font-size: 14px; font-weight: 500;
  z-index: 999; pointer-events: none; backdrop-filter: blur(8px);
}
.toast-enter-active { transition: all .3s ease-out; }
.toast-leave-active { transition: all .3s ease-in; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, -50%) scale(.85); }

/* ──── Shared states ──── */
.loading-state, .empty-state { text-align: center; padding: 100px 20px; color: #94a3b8; }
.empty-icon { font-size: 56px; margin-bottom: 12px; }
.empty-link { display: inline-block; margin-top: 16px; padding: 8px 24px; background: #f97316; color: #fff; border-radius: 20px; text-decoration: none; font-size: 14px; }
</style>
