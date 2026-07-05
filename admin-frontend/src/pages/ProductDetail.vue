<template>
  <div class="max-w-4xl mx-auto space-y-6 animate-fade-in">
    <div v-if="loading" class="text-center py-20 text-gray-400">加载中...</div>

    <template v-else-if="product">
      <!-- 商品主图 + 基本信息 -->
      <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
        <div class="md:flex">
          <div class="md:w-1/2 aspect-square bg-gray-100 flex items-center justify-center">
            <img v-if="product.image" :src="product.image" :alt="product.name"
              class="w-full h-full object-cover" />
            <span v-else class="text-6xl text-gray-300">📦</span>
          </div>
          <div class="md:w-1/2 p-6 flex flex-col">
            <h1 class="text-xl font-bold text-gray-800">{{ product.name }}</h1>
            <p v-if="product.description" class="text-sm text-gray-500 mt-2">{{ product.description }}</p>

            <div class="mt-4 flex items-baseline gap-1">
              <span class="text-2xl font-bold text-red-500">¥{{ selectedPrice }}</span>
              <span v-if="maxPrice > minPrice" class="text-sm text-gray-400">~ ¥{{ maxPrice }}</span>
            </div>

            <div class="mt-4 space-y-1 text-sm text-gray-500">
              <div class="flex items-center gap-2">
                <span>店铺：{{ product.shopName || '-' }}</span>
                <router-link
                  :to="`/shops/${product.shopId}?name=${encodeURIComponent(product.shopName || '')}`"
                  class="inline-flex items-center text-xs px-2 py-0.5 rounded bg-indigo-50 text-indigo-600 hover:bg-indigo-100 transition-colors font-medium">
                  进店 ▸
                </router-link>
              </div>
              <div>分类：{{ product.categoryName || '-' }}</div>
              <div>库存：{{ selectedSku ? selectedSku.stock : product.totalStock }}</div>
              <div>销量：{{ product.sales || 0 }}</div>
            </div>

            <!-- 规格选择 -->
            <div v-if="product.skus && product.skus.length > 0" class="mt-4 space-y-3">
              <div v-if="specGroups.length > 0" v-for="group in specGroups" :key="group.name" class="flex items-center gap-2">
                <span class="text-sm text-gray-500 w-10">{{ group.name }}：</span>
                <div class="flex flex-wrap gap-1.5">
                  <button
                    v-for="val in group.values"
                    :key="val"
                    @click="selectSpec(group.name, val)"
                    class="px-3 py-1 text-xs rounded-md border transition-colors"
                    :class="selectedSpecs[group.name] === val
                      ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                      : 'border-gray-200 text-gray-600 hover:border-gray-300'"
                  >{{ val }}</button>
                </div>
              </div>
            </div>

            <!-- 数量 -->
            <div class="mt-4 flex items-center gap-2">
              <span class="text-sm text-gray-500">数量：</span>
              <button @click="qty > 1 && qty--"
                class="w-7 h-7 rounded border border-gray-200 text-gray-500 hover:border-gray-300 text-sm">−</button>
              <input v-model.number="qty" class="w-14 text-center border border-gray-200 rounded py-0.5 text-sm" min="1" />
              <button @click="qty < maxQty && qty++"
                class="w-7 h-7 rounded border border-gray-200 text-gray-500 hover:border-gray-300 text-sm">+</button>
            </div>

            <!-- 购买按钮 -->
            <div class="mt-6 flex gap-3">
              <button @click="buyNow" :disabled="buying"
                class="flex-1 px-6 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50">
                {{ buying ? '下单中...' : '立即购买' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- SKU 规格列表 -->
      <div v-if="product.skus && product.skus.length > 0" class="bg-white rounded-xl border border-gray-100 p-5">
        <h3 class="font-semibold text-gray-800 mb-3">规格列表</h3>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="bg-gray-50 text-left">
              <tr>
                <th class="px-4 py-2 font-medium text-gray-500">规格</th>
                <th class="px-4 py-2 font-medium text-gray-500">价格</th>
                <th class="px-4 py-2 font-medium text-gray-500">库存</th>
                <th class="px-4 py-2 font-medium text-gray-500">编码</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="sku in product.skus" :key="sku.id"
                @click="selectSkuRow(sku)"
                class="cursor-pointer hover:bg-indigo-50/50 transition-colors"
                :class="selectedSku && selectedSku.id === sku.id ? 'bg-indigo-50' : ''">
                <td class="px-4 py-2 text-gray-700">
                  <span v-if="sku.specs && sku.specs.length > 0">
                    <span v-for="(spec, i) in sku.specs" :key="i">
                      <span class="text-gray-400">{{ spec.name }}:</span>
                      {{ spec.value }}<span v-if="i < sku.specs.length - 1">，</span>
                    </span>
                  </span>
                  <span v-else class="text-gray-400">默认</span>
                </td>
                <td class="px-4 py-2 font-medium text-red-500">¥{{ (sku.price / 100).toFixed(2) }}</td>
                <td class="px-4 py-2 text-gray-600">{{ sku.stock }}</td>
                <td class="px-4 py-2 text-gray-400 text-xs font-mono">{{ sku.skuCode || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <div v-else class="bg-white rounded-xl border border-gray-100 py-20 text-center text-gray-400">
      商品不存在或已下架
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productAPI, orderAPI } from '@/api'

const route = useRoute()
const router = useRouter()
const productId = computed(() => Number(route.params.id))

const product = ref(null)
const loading = ref(false)
const buying = ref(false)
const qty = ref(1)
const selectedSku = ref(null)
const selectedSpecs = ref({})

const minPrice = computed(() => {
  const skus = product.value?.skus
  if (!skus || skus.length === 0) return product.value?.minPrice ? (product.value.minPrice / 100).toFixed(2) : '0.00'
  return (Math.min(...skus.map(s => s.price)) / 100).toFixed(2)
})

const maxPrice = computed(() => {
  const skus = product.value?.skus
  if (!skus || skus.length === 0) return null
  return (Math.max(...skus.map(s => s.price)) / 100).toFixed(2)
})

const selectedPrice = computed(() => {
  if (selectedSku.value) return (selectedSku.value.price / 100).toFixed(2)
  return minPrice.value
})

const maxQty = computed(() => {
  if (selectedSku.value) return selectedSku.value.stock
  return product.value?.totalStock || 1
})

const specGroups = computed(() => {
  const skus = product.value?.skus
  if (!skus || skus.length === 0) return []
  const groups = {}
  skus.forEach(sku => {
    if (!sku.specs) return
    sku.specs.forEach(spec => {
      if (!groups[spec.name]) groups[spec.name] = new Set()
      groups[spec.name].add(spec.value)
    })
  })
  return Object.entries(groups).map(([name, values]) => ({ name, values: [...values] }))
})

function selectSpec(name, value) {
  selectedSpecs.value = { ...selectedSpecs.value, [name]: value }
  matchSku()
}

function matchSku() {
  const skus = product.value?.skus || []
  if (skus.length === 0) return
  if (skus.length === 1) { selectedSku.value = skus[0]; return }

  const specs = selectedSpecs.value
  const specKeys = Object.keys(specs)
  if (specKeys.length === 0) { selectedSku.value = skus[0]; return }

  const matched = skus.find(sku => {
    if (!sku.specs) return false
    return specKeys.every(key => {
      const spec = sku.specs.find(s => s.name === key)
      return spec && spec.value === specs[key]
    })
  })
  if (matched) selectedSku.value = matched
}

function selectSkuRow(sku) {
  selectedSku.value = sku
  if (sku.specs && sku.specs.length > 0) {
    const specs = {}
    sku.specs.forEach(s => { specs[s.name] = s.value })
    selectedSpecs.value = specs
  }
}

async function buyNow() {
  const sku = selectedSku.value
  if (!sku && product.value?.skus?.length > 1) {
    ElMessage.warning('请选择规格')
    return
  }

  buying.value = true
  try {
    const body = {
      productId: productId.value,
      skuId: sku ? sku.id : 0,
      quantity: qty.value,
      addressId: null,
      couponId: null,
      remark: '',
    }
    const orderNo = await orderAPI.buyNow(body)
    ElMessage.success('下单成功，正在跳转支付...')
    router.push(`/pay?orderNo=${orderNo}`)
  } catch (e) {
    ElMessage.error(e.message || '下单失败')
  } finally {
    buying.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    product.value = await productAPI.getDetail(productId.value)
    if (product.value?.skus?.length > 0) {
      selectedSku.value = product.value.skus[0]
    }
  } catch (e) {
    console.error('加载商品详情失败:', e)
  } finally {
    loading.value = false
  }
})
</script>
