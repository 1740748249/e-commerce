<template>
  <div class="px-4 md:px-0">
    <!-- Hero Banner (移动端) -->
    <div class="md:hidden mb-4 bg-gradient-to-br from-orange-400 via-orange-500 to-amber-500 rounded-2xl px-5 py-4 shadow-lg shadow-orange-200/50">
      <h1 class="text-lg font-bold text-white">发现好物</h1>
      <p class="text-xs text-orange-100 mt-1">精选优质商品，等你来逛</p>
    </div>

    <!-- 搜索 + 分类 + 排序 -->
    <div class="mb-4 md:mb-6">
      <div class="relative mb-3 md:mb-4">
        <input
          v-model="keyword"
          placeholder="搜索商品名称或店铺..."
          class="w-full border-0 rounded-2xl px-5 py-3.5 md:py-4 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 bg-white shadow-md hover:shadow-lg transition-shadow pr-12"
          @keyup.enter="doSearch"
        />
        <span @click="doSearch" class="absolute right-4 top-1/2 -translate-y-1/2 w-9 h-9 bg-gradient-to-br from-orange-400 to-orange-500 rounded-xl flex items-center justify-center text-white text-sm shadow-md cursor-pointer">
          &#128269;
        </span>
      </div>

      <!-- PC 分类 + 排序 -->
      <div class="hidden md:flex gap-2 flex-wrap items-center justify-between">
        <div class="flex gap-2 flex-wrap">
          <button
            v-for="c in categoryItems"
            :key="c.id || c"
            @click="selectCategory(c)"
            :class="[
              'px-5 py-2 rounded-xl text-sm font-medium transition-all duration-300',
              activeCatId === (c.id || c)
                ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-md shadow-orange-200'
                : 'bg-white text-gray-600 hover:bg-orange-50 hover:text-orange-600 shadow-sm'
            ]"
          >
            {{ c.name || c }}
          </button>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-xs text-gray-400">排序：</span>
          <select
            v-model="sortBy"
            @change="doSearch"
            class="text-sm border-0 bg-white rounded-xl px-3 py-2 shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-300 text-gray-600"
          >
            <option value="">默认</option>
            <option value="price_asc">价格从低到高</option>
            <option value="price_desc">价格从高到低</option>
            <option value="sales_desc">销量优先</option>
          </select>
        </div>
      </div>

      <!-- 移动端分类横向滚动 -->
      <div class="md:hidden flex gap-2 overflow-x-auto pb-1 -mx-1 px-1 scrollbar-hide items-center">
        <button
          v-for="c in categoryItems"
          :key="c.id || c"
          @click="selectCategory(c)"
          :class="[
            'px-4 py-2 rounded-xl text-xs whitespace-nowrap shrink-0 transition-all duration-300',
            activeCatId === (c.id || c)
              ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-md'
              : 'bg-white text-gray-600 shadow-sm'
          ]"
        >
          {{ c.name || c }}
        </button>
        <select
          v-model="sortBy"
          @change="doSearch"
          class="text-xs border-0 bg-white rounded-xl px-3 py-2 shadow-sm focus:outline-none text-gray-600 shrink-0"
        >
          <option value="">默认</option>
          <option value="price_asc">价格↑</option>
          <option value="price_desc">价格↓</option>
          <option value="sales_desc">销量</option>
        </select>
      </div>
    </div>

    <!-- 秒杀模块 -->
    <FlashSale />

    <!-- 商品网格 -->
    <div v-if="loading" class="text-center py-12 text-gray-400">
      <el-icon class="is-loading text-2xl text-orange-400"><span>&#8635;</span></el-icon>
      <p class="text-sm mt-2">加载中...</p>
    </div>

    <div v-else class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 md:gap-5">
      <div
        v-for="(p, index) in visibleList"
        :key="p.id"
        @click="$router.push(`/product/${p.id}`)"
        class="product-card bg-white rounded-2xl overflow-hidden shadow-md cursor-pointer group animate-fade-in-up"
        :style="{ animationDelay: `${index * 50}ms` }"
      >
        <div class="relative overflow-hidden">
          <img
            :src="p.image"
            :alt="p.name"
            class="w-full aspect-square object-cover transition-transform duration-500 group-hover:scale-110"
            loading="lazy"
          />
          <div class="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
          <div v-if="p.sales > 50" class="absolute top-2 left-2">
            <span class="bg-gradient-to-r from-red-500 to-orange-500 text-white text-[10px] px-2 py-0.5 rounded-full shadow-md">
              热卖
            </span>
          </div>
        </div>
        <div class="p-3 md:p-4">
          <h3 class="text-xs md:text-sm font-medium text-gray-800 truncate group-hover:text-orange-600 transition-colors">
            {{ p.name }}
          </h3>
          <p class="text-[10px] md:text-xs text-gray-400 mt-1 flex items-center gap-1">
            <span class="w-1.5 h-1.5 bg-green-400 rounded-full"></span>
            {{ p.shopName }}
          </p>
          <div class="flex items-center justify-between mt-2 md:mt-3">
            <div class="flex items-baseline gap-0.5">
              <span class="text-xs text-orange-500">¥</span>
              <span class="text-base md:text-xl font-bold text-orange-500">{{ (p.price / 100).toFixed(0) }}</span>
            </div>
            <span class="text-[10px] md:text-xs text-gray-400">已售 {{ p.sales }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && list.length === 0" class="text-center py-24">
      <div class="w-20 h-20 mx-auto mb-4 bg-orange-100 rounded-full flex items-center justify-center text-4xl">
        &#128270;
      </div>
      <p class="text-gray-500 font-medium">没有找到相关商品</p>
      <p class="text-sm text-gray-400 mt-1">换个关键词试试吧</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, onDeactivated } from 'vue'
import FlashSale from '@/components/FlashSale.vue'
import { useProductStore } from '@/stores/product'
import { savedPositions } from '@/router'

defineOptions({ name: 'Home' })

const productStore = useProductStore()

const keyword = ref('')
const activeCatId = ref('')
const sortBy = ref('')
const loading = ref(false)

const list = computed(() => productStore.list)

const categoryItems = computed(() => {
  const cats = productStore.categories
  return cats.length ? [{ id: '', name: '全部' }, ...cats] : [{ id: '', name: '全部' }]
})

const visibleList = computed(() => list.value)

function selectCategory(c) {
  activeCatId.value = c.id || ''
  doSearch()
}

async function doSearch() {
  loading.value = true
  const params = { page: 1, size: 50 }
  if (keyword.value) params.keyword = keyword.value
  if (activeCatId.value) params.categoryId = activeCatId.value
  if (sortBy.value) params.sort = sortBy.value
  await productStore.fetchList(params)
  loading.value = false
}

onMounted(async () => {
  await Promise.all([productStore.fetchCategories(), doSearch()])
})

onActivated(() => {
  const pos = savedPositions['Home']
  if (pos) {
    window.scrollTo(pos.left, pos.top)
  }
})

onDeactivated(() => {
  savedPositions['Home'] = { top: window.scrollY, left: 0 }
})
</script>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
