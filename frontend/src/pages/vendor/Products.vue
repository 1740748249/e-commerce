<template>
  <div v-if="auth.isVendor" class="px-4 md:px-0">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between mb-5">
      <div class="flex items-center gap-3">
        <router-link to="/vendor" class="md:hidden text-gray-400 active:text-gray-600 text-xl">&larr;</router-link>
        <h1 class="text-lg md:text-xl font-bold text-gray-800">商品管理</h1>
        <span class="text-sm text-gray-400">({{ productStore.list.length }} 件)</span>
      </div>
      <button
        @click="openAdd"
        class="flex items-center gap-1.5 px-4 py-2.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl text-sm font-medium hover:shadow-lg hover:shadow-orange-200 transition-all duration-300 hover:-translate-y-0.5"
      >
        <span>+</span> 添加商品
      </button>
    </div>

    <!-- PC 端表格 -->
    <div class="hidden md:block bg-white rounded-2xl shadow-md overflow-hidden">
      <div v-if="productStore.list.length === 0" class="text-center py-16">
        <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center text-3xl">&#128230;</div>
        <p class="text-gray-400">还没有商品，立即添加</p>
        <button @click="openAdd" class="mt-4 px-6 py-2 bg-orange-500 text-white rounded-lg text-sm font-medium">添加商品</button>
      </div>
      <table v-else class="w-full text-sm">
        <thead>
          <tr class="border-b text-left text-gray-400 bg-gray-50/50">
            <th class="py-4 px-5 font-medium">商品</th>
            <th class="py-4 px-5 font-medium">分类</th>
            <th class="py-4 px-5 font-medium">价格</th>
            <th class="py-4 px-5 font-medium">库存</th>
            <th class="py-4 px-5 font-medium">状态</th>
            <th class="py-4 px-5 font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in productStore.list" :key="p.id" class="border-b last:border-0 hover:bg-gray-50/50 transition-colors">
            <td class="py-4 px-5">
              <div class="flex items-center gap-3">
                <img :src="p.image" class="w-12 h-12 rounded-xl object-cover shadow-sm" />
                <span class="font-medium text-gray-800">{{ p.name }}</span>
              </div>
            </td>
            <td class="py-4 px-5 text-gray-500">{{ p.category }}</td>
            <td class="py-4 px-5 text-orange-500 font-bold">¥{{ (p.price / 100).toFixed(2) }}</td>
            <td class="py-4 px-5 text-gray-600">{{ p.stock }}</td>
            <td class="py-4 px-5">
              <el-switch
                :model-value="p.status !== 'off'"
                @change="toggleStatus(p)"
                active-text="上架"
                inactive-text="下架"
                size="small"
              />
            </td>
            <td class="py-4 px-5">
              <div class="flex gap-2">
                <button @click="openEdit(p)" class="px-3 py-1.5 text-blue-600 hover:bg-blue-50 rounded-lg text-xs font-medium transition-colors">编辑</button>
                <button @click="deleteProduct(p)" class="px-3 py-1.5 text-red-500 hover:bg-red-50 rounded-lg text-xs font-medium transition-colors">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 移动端卡片 -->
    <div class="md:hidden space-y-3">
      <div v-if="productStore.list.length === 0" class="text-center py-16 bg-white rounded-2xl">
        <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center text-3xl">&#128230;</div>
        <p class="text-gray-400">还没有商品，立即添加</p>
      </div>
      <div v-for="p in productStore.list" :key="p.id" class="bg-white rounded-2xl p-4 shadow-md">
        <div class="flex gap-3">
          <img :src="p.image" class="w-16 h-16 rounded-xl object-cover shadow-sm shrink-0" />
          <div class="flex-1 min-w-0">
            <p class="font-medium text-gray-800 truncate">{{ p.name }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ p.category }}</p>
            <div class="flex items-center justify-between mt-2">
              <span class="text-orange-500 font-bold">¥{{ (p.price / 100).toFixed(2) }}</span>
              <span class="text-xs text-gray-400">库存 {{ p.stock }}</span>
            </div>
          </div>
        </div>
        <div class="flex items-center justify-between mt-3 pt-3 border-t border-gray-100">
          <el-switch
            :model-value="p.status !== 'off'"
            @change="toggleStatus(p)"
            active-text="上架"
            inactive-text="下架"
            size="small"
          />
          <div class="flex gap-2">
            <button @click="openEdit(p)" class="text-blue-600 text-xs font-medium">编辑</button>
            <button @click="deleteProduct(p)" class="text-red-500 text-xs font-medium">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑弹窗 -->
    <div v-if="showEdit" class="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4" @click.self="closeEdit">
      <div class="bg-white rounded-2xl p-6 w-full max-w-lg shadow-2xl animate-fade-in-up max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg font-bold text-gray-800 mb-5">{{ editP.id ? '编辑商品' : '添加商品' }}</h2>
        <div class="space-y-4">
          <!-- 商品名称 -->
          <div>
            <label class="text-sm text-gray-600 font-medium">商品名称</label>
            <input v-model="editP.name" class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 mt-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all" placeholder="请输入商品名称" />
          </div>
          <!-- 分类 -->
          <div>
            <label class="text-sm text-gray-600 font-medium">分类</label>
            <select v-model="editP.categoryId" class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 mt-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all">
              <option v-for="c in productStore.categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <!-- 商品描述 -->
          <div>
            <label class="text-sm text-gray-600 font-medium">商品描述</label>
            <textarea v-model="editP.desc" class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 mt-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all resize-none" placeholder="请输入商品描述" rows="3"></textarea>
          </div>
          <!-- 主图上传 -->
          <div>
            <label class="text-sm text-gray-600 font-medium">商品主图</label>
            <div class="flex items-center gap-3 mt-2">
              <div class="relative w-20 h-20 rounded-xl bg-gray-100 border-2 border-dashed cursor-pointer overflow-hidden transition-all duration-200 hover:border-orange-400 hover:bg-orange-50" :class="editP.image ? 'border-solid border-gray-200' : 'border-gray-300'" @click="$refs.mainImageInput.click()">
                <img v-if="editP.image" :src="editP.image" class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex flex-col items-center justify-center text-gray-400"><span class="text-lg">+</span></div>
              </div>
              <input ref="mainImageInput" type="file" accept="image/*" class="hidden" @change="onMainImageChange" />
              <span v-if="mainImageUploading" class="text-xs text-orange-500">上传中...</span>
            </div>
          </div>

          <!-- 规格管理 -->
          <div class="border-t border-gray-100 pt-4">
            <div class="flex items-center justify-between mb-3">
              <label class="text-sm text-gray-600 font-medium">商品规格</label>
              <button @click="showAddSpecInput = true" class="text-xs text-orange-500 font-medium hover:text-orange-600">+ 添加规格</button>
            </div>

            <!-- 新规格名输入 -->
            <div v-if="showAddSpecInput" class="flex items-center gap-2 mb-3 bg-gray-50 rounded-xl p-3">
              <input v-model="newSpecName" @keyup.enter="confirmAddSpec" class="flex-1 bg-white border-0 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300" placeholder="输入规格名，如：颜色" />
              <button @click="confirmAddSpec" class="px-3 py-2 bg-orange-500 text-white rounded-lg text-xs font-medium hover:bg-orange-600 transition-colors shrink-0">确认</button>
              <button @click="showAddSpecInput = false; newSpecName = ''" class="px-3 py-2 bg-gray-200 text-gray-600 rounded-lg text-xs font-medium hover:bg-gray-300 transition-colors shrink-0">取消</button>
            </div>

            <div v-if="specList.length === 0 && !showAddSpecInput" class="text-xs text-gray-400 bg-gray-50 rounded-xl p-4 text-center">
              暂未添加规格，商品将创建一个默认 SKU
            </div>
            <div v-for="(specName, si) in specList" :key="specName" class="bg-gray-50 rounded-xl p-3 mb-2">
              <div class="flex items-center gap-2 mb-2">
                <span class="text-sm font-medium text-gray-700">{{ specName }}</span>
                <button @click="removeSpec(si)" class="text-gray-400 hover:text-red-500 text-xs ml-auto">删除规格</button>
              </div>
              <div class="flex flex-wrap gap-2">
                <div v-for="(opt, oi) in (specOptions[specName] || [])" :key="oi" class="flex items-center gap-1 bg-white rounded-lg px-2 py-1">
                  <input v-model="specOptions[specName][oi]" class="w-16 text-xs bg-transparent border-0 focus:outline-none" placeholder="选项值" />
                  <button @click="removeSpecOption(specName, oi)" class="text-gray-300 hover:text-red-400 text-xs">&times;</button>
                </div>
                <button @click="addSpecOption(specName)" class="text-xs text-orange-400 hover:text-orange-500 px-2 py-1">+</button>
              </div>
            </div>
          </div>

          <!-- SKU 表格 -->
          <div class="border-t border-gray-100 pt-4">
            <label class="text-sm text-gray-600 font-medium mb-3 block">SKU 明细（{{ skuRows.length }} 个）</label>
            <div class="overflow-x-auto -mx-1">
              <table class="w-full text-xs">
                <thead>
                  <tr class="text-left text-gray-400 bg-gray-50">
                    <th v-for="(name, gi) in specList" :key="'h'+gi" class="py-2 px-2 font-medium whitespace-nowrap">{{ name || '规格'+(gi+1) }}</th>
                    <th class="py-2 px-2 font-medium whitespace-nowrap">SKU编码</th>
                    <th class="py-2 px-2 font-medium whitespace-nowrap">价格(元)</th>
                    <th class="py-2 px-2 font-medium whitespace-nowrap">库存</th>
                    <th class="py-2 px-2 font-medium whitespace-nowrap">图片</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(sku, si) in skuRows" :key="si" class="border-b border-gray-50">
                    <td v-for="(name, gi) in specList" :key="'s'+gi" class="py-1.5 px-2 text-gray-600">{{ sku.specs[gi]?.value || '-' }}</td>
                    <td class="py-1.5 px-2">
                      <input v-model="sku.skuCode" class="w-20 border-0 bg-gray-50 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-orange-300" placeholder="自动" />
                    </td>
                    <td class="py-1.5 px-2">
                      <input v-model.number="sku.price" type="number" step="0.01" min="0" class="w-20 border-0 bg-gray-50 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-orange-300" placeholder="0.00" />
                    </td>
                    <td class="py-1.5 px-2">
                      <input v-model.number="sku.stock" type="number" min="0" class="w-16 border-0 bg-gray-50 rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-orange-300" placeholder="0" />
                    </td>
                    <td class="py-1.5 px-2">
                      <div class="flex items-center gap-1">
                        <div class="w-8 h-8 rounded bg-gray-100 cursor-pointer overflow-hidden flex-shrink-0" @click="$refs[`skuImg_${si}`]?.[0]?.click()">
                          <img v-if="sku.image" :src="sku.image" class="w-full h-full object-cover" />
                          <span v-else class="w-full h-full flex items-center justify-center text-gray-400 text-xs">+</span>
                        </div>
                        <input :ref="`skuImg_${si}`" type="file" accept="image/*" class="hidden" @change="(e) => onSkuImageChange(e, si)" />
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button @click="closeEdit" class="px-5 py-2.5 border-2 border-gray-200 rounded-xl text-sm text-gray-600 font-medium hover:bg-gray-50 transition-colors">取消</button>
          <button @click="saveProduct" :disabled="saving" class="px-5 py-2.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl text-sm font-medium hover:shadow-lg hover:shadow-orange-200 transition-all disabled:opacity-60">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>

  <!-- 非商家提示 -->
  <div v-else class="text-center py-24">
    <p class="text-gray-400">请使用商家账号<a href="/login" class="text-orange-500">登录</a></p>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useProductStore } from '@/stores/product'
import { fileAPI } from '@/api'

const auth = useAuthStore()
const productStore = useProductStore()
const showEdit = ref(false)
const editP = ref({})
const saving = ref(false)
const specList = ref([])
const specOptions = ref({})
const showAddSpecInput = ref(false)
const newSpecName = ref('')
const mainImageUploading = ref(false)

// --- spec / sku helpers ---
function genSkuRows() {
  const names = specList.value.filter(n => {
    const opts = specOptions.value[n] || []
    return opts.some(o => o && o.trim())
  })
  if (!names.length) {
    return [{ specs: [], skuCode: '', price: 0, stock: 0, image: '' }]
  }
  let combos = [[]]
  names.forEach(name => {
    const opts = (specOptions.value[name] || []).filter(o => o && o.trim())
    if (!opts.length) return
    const next = []
    combos.forEach(a => opts.forEach(o => next.push([...a, { name, value: o }])))
    combos = next
  })
  return combos.map(specs => ({ specs, skuCode: '', price: 0, stock: 0, image: '' }))
}

const skuRows = ref([{ specs: [], skuCode: '', price: 0, stock: 0, image: '' }])

let syncLock = false
watch([specList, specOptions], () => {
  if (syncLock) return
  const newRows = genSkuRows()
  const oldRows = skuRows.value
  skuRows.value = newRows.map(newRow => {
    if (!newRow.specs.length) {
      return oldRows.length === 1 && !oldRows[0].specs.length ? oldRows[0] : newRow
    }
    const match = oldRows.find(oldRow => {
      if (oldRow.specs.length !== newRow.specs.length) return false
      return newRow.specs.every(sp =>
        oldRow.specs.some(rs => rs.name === sp.name && rs.value === sp.value)
      )
    })
    return match
      ? { ...newRow, id: match.id, price: match.price, stock: match.stock, skuCode: match.skuCode, image: match.image }
      : newRow
  })
}, { deep: true, flush: 'sync' })

function confirmAddSpec() {
  const name = newSpecName.value.trim()
  if (!name) return
  if (specList.value.includes(name)) {
    ElMessage.warning('规格名已存在')
    return
  }
  specList.value.push(name)
  specOptions.value[name] = ['']
  newSpecName.value = ''
  showAddSpecInput.value = false
}

function removeSpec(index) {
  const name = specList.value[index]
  specList.value.splice(index, 1)
  delete specOptions.value[name]
}

function addSpecOption(specName) {
  specOptions.value[specName].push('')
}

function removeSpecOption(specName, optIndex) {
  specOptions.value[specName].splice(optIndex, 1)
  if (specOptions.value[specName].length === 0) {
    specOptions.value[specName] = ['']
  }
}

async function onMainImageChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  mainImageUploading.value = true
  try {
    const res = await fileAPI.upload(file, 'product')
    editP.value.image = res.url
    ElMessage.success('主图上传成功')
  } catch (err) {
    ElMessage.error('主图上传失败: ' + (err.message || '未知错误'))
  } finally {
    mainImageUploading.value = false
  }
}
async function onSkuImageChange(e, si) {
  const file = e.target.files?.[0]
  if (!file) return
  try {
    const res = await fileAPI.upload(file, 'sku')
    skuRows.value[si].image = res.url
    ElMessage.success('SKU 图片上传成功')
  } catch (err) {
    ElMessage.error('SKU 图片上传失败: ' + (err.message || '未知错误'))
  }
}

// --- form ---
function getDefaultCategoryId() {
  return productStore.categories[0]?.id || 0
}

function openAdd() {
  syncLock = true
  editP.value = { id: 0, name: '', categoryId: getDefaultCategoryId(), desc: '', image: '' }
  specList.value = []
  specOptions.value = {}
  showAddSpecInput.value = false
  newSpecName.value = ''
  skuRows.value = [{ specs: [], skuCode: '', price: 0, stock: 0, image: '' }]
  showEdit.value = true
  syncLock = false
}

function openEdit(p) {
  syncLock = true
  editP.value = {
    id: p.id,
    name: p.name,
    categoryId: p.categoryId || getDefaultCategoryId(),
    desc: p.desc,
    image: p.image,
  }
  showAddSpecInput.value = false
  newSpecName.value = ''
  const skus = p.skus || []
  if (skus.length) {
    const nameSet = new Set()
    const optsMap = {}
    skus.forEach(sku => {
      const specs = Array.isArray(sku.specs)
        ? sku.specs
        : (sku.specs && typeof sku.specs === 'object' ? Object.entries(sku.specs).map(([k, v]) => ({ name: k, value: v })) : [])
      specs.forEach(sp => {
        nameSet.add(sp.name)
        if (!optsMap[sp.name]) optsMap[sp.name] = new Set()
        optsMap[sp.name].add(sp.value)
      })
    })
    specList.value = [...nameSet]
    const newSpecOptions = {}
    nameSet.forEach(name => {
      newSpecOptions[name] = [...optsMap[name]]
    })
    specOptions.value = newSpecOptions
    skuRows.value = skus.map(sku => {
      const s = Array.isArray(sku.specs)
        ? sku.specs
        : (sku.specs && typeof sku.specs === 'object' ? Object.entries(sku.specs).map(([k, v]) => ({ name: k, value: v })) : [])
      return { id: sku.id || 0, specs: s, skuCode: sku.sku_code || sku.skuCode || '', price: Number((sku.price / 100).toFixed(2)), stock: sku.stock, image: sku.image || '' }
    })
  } else {
    specList.value = []
    specOptions.value = {}
    skuRows.value = [{ specs: [], skuCode: '', price: 0, stock: 0, image: '' }]
  }
  syncLock = false
  showEdit.value = true
}

function closeEdit() { showEdit.value = false }

async function saveProduct() {
  if (!editP.value.name) { ElMessage.warning('请输入商品名称'); return }
  const hasValidSku = skuRows.value.some(s => s.price > 0 && s.stock > 0)
  if (!hasValidSku) { ElMessage.warning('请至少填写一个 SKU 的价格和库存'); return }

  saving.value = true
  try {
    const body = {
      name: editP.value.name,
      categoryId: editP.value.categoryId,
      description: editP.value.desc,
      image: editP.value.image,
      skus: skuRows.value.map(s => ({
        id: s.id || undefined,
        skuCode: s.skuCode || undefined,
        specs: s.specs.length ? s.specs.map(sp => ({ name: sp.name, value: sp.value })) : [],
        price: Math.round(s.price * 100),
        stock: s.stock,
        image: s.image || undefined,
      })),
    }
    if (editP.value.id) {
      await productStore.updateProduct(editP.value.id, body)
      ElMessage.success('商品已更新')
    } else {
      await productStore.addProduct(body)
      ElMessage.success('商品已添加')
    }
    closeEdit()
  } catch (e) {
    ElMessage.error(e.message || '保存失败，请稍后重试')
  }
  saving.value = false
}

async function toggleStatus(p) {
  try {
    await productStore.toggleStatus(p.id, p.status)
    ElMessage.success(p.status !== 'off' ? '已下架' : '已上架')
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

async function deleteProduct(p) {
  try { await ElMessageBox.confirm('确定删除该商品吗？', '删除商品', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }) } catch { return }
  try {
    await productStore.deleteProduct(p.id)
    ElMessage.success('商品已删除')
  } catch (e) { ElMessage.error(e.message || '删除失败') }
}

onMounted(async () => {
  await Promise.all([productStore.fetchMyProducts(), productStore.fetchCategories()])
})
</script>
