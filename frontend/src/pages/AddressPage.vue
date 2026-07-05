<template>
  <div v-if="auth.isLoggedIn" class="max-w-lg mx-auto px-4 md:px-0">
    <button @click="$router.back()" class="text-gray-400 hover:text-gray-600 text-sm mb-4 flex items-center gap-1 transition-colors">&larr; 返回</button>
    <div class="flex items-center justify-between mb-5">
      <h1 class="text-xl font-bold text-gray-800">收货地址</h1>
      <button
        @click="openAdd"
        class="flex items-center gap-1 px-4 py-2 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl text-sm font-medium hover:shadow-lg hover:shadow-orange-200 transition-all"
      >
        <span>+</span> 新增地址
      </button>
    </div>

    <!-- 加载中 -->
    <div v-if="addr.loading" class="text-center py-12 text-gray-400">
      <el-icon class="is-loading text-2xl text-orange-400"><span>&#8635;</span></el-icon>
      <p class="text-sm mt-2">加载中...</p>
    </div>

    <!-- 地址列表 -->
    <div v-else-if="addr.addresses.length > 0" class="space-y-3">
      <div
        v-for="a in addr.addresses"
        :key="a.id"
        :class="[
          'bg-white rounded-2xl p-4 shadow-md border-2 transition-all',
          a.isDefault ? 'border-orange-300' : 'border-transparent'
        ]"
      >
        <div class="flex items-center justify-between mb-2">
          <div class="flex items-center gap-2">
            <span class="font-medium text-gray-800">{{ a.name }}</span>
            <span class="text-sm text-gray-400">{{ a.phone }}</span>
            <span v-if="a.isDefault" class="text-[10px] text-orange-500 bg-orange-50 px-2 py-0.5 rounded-full">默认</span>
          </div>
          <div class="flex gap-2">
            <button @click="openEdit(a)" class="text-sm text-blue-500 hover:text-blue-600">编辑</button>
            <button @click="removeAddr(a)" class="text-sm text-red-400 hover:text-red-500">删除</button>
          </div>
        </div>
        <p class="text-sm text-gray-500">{{ a.label }}</p>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-16 bg-white rounded-2xl">
      <div class="w-16 h-16 mx-auto mb-4 bg-gray-100 rounded-full flex items-center justify-center text-3xl">
        &#128205;
      </div>
      <p class="text-gray-400">还没有收货地址</p>
      <button
        @click="openAdd"
        class="mt-4 px-6 py-2 bg-orange-500 text-white rounded-xl text-sm font-medium"
      >
        新增地址
      </button>
    </div>

    <!-- 添加/编辑弹窗 -->
    <div v-if="showEdit" class="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4" @click.self="closeEdit">
      <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-2xl animate-fade-in-up">
        <h2 class="text-lg font-bold text-gray-800 mb-5">{{ editing.id ? '编辑地址' : '新增地址' }}</h2>
        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-xs text-gray-400 mb-1 block">收货人</label>
              <input
                v-model="editing.name"
                class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all"
                placeholder="姓名"
              />
            </div>
            <div>
              <label class="text-xs text-gray-400 mb-1 block">手机号</label>
              <input
                v-model="editing.phone"
                class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all"
                placeholder="手机号"
              />
            </div>
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">省/市/区</label>
            <div class="grid grid-cols-3 gap-2">
              <input
                v-model="editing.province"
                class="border-0 bg-gray-50 rounded-xl px-3 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all"
                placeholder="省"
              />
              <input
                v-model="editing.city"
                class="border-0 bg-gray-50 rounded-xl px-3 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all"
                placeholder="市"
              />
              <input
                v-model="editing.district"
                class="border-0 bg-gray-50 rounded-xl px-3 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all"
                placeholder="区"
              />
            </div>
          </div>
          <div>
            <label class="text-xs text-gray-400 mb-1 block">详细地址</label>
            <input
              v-model="editing.detail"
              class="w-full border-0 bg-gray-50 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:bg-white transition-all"
              placeholder="街道、门牌号等"
            />
          </div>
          <label class="flex items-center gap-2 cursor-pointer">
            <input v-model="editing.isDefault" type="checkbox" class="w-4 h-4 text-orange-500 rounded" />
            <span class="text-sm text-gray-600">设为默认地址</span>
          </label>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="closeEdit"
            class="px-5 py-2.5 border-2 border-gray-200 rounded-xl text-sm text-gray-600 font-medium hover:bg-gray-50 transition-colors"
          >
            取消
          </button>
          <button
            @click="saveAddr"
            :disabled="saving"
            class="px-5 py-2.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl text-sm font-medium hover:shadow-lg hover:shadow-orange-200 transition-all disabled:opacity-60"
          >
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 未登录 -->
  <div v-else class="text-center py-24">
    <div class="w-20 h-20 mx-auto mb-6 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center">
      <span class="text-3xl">&#128100;</span>
    </div>
    <p class="text-gray-500 font-medium">请先登录</p>
    <router-link to="/login" class="inline-block mt-4 px-6 py-2.5 bg-orange-500 text-white rounded-xl text-sm font-medium">去登录</router-link>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useAddressStore } from '@/stores/address'

const auth = useAuthStore()
const addr = useAddressStore()

const showEdit = ref(false)
const editing = ref({})
const saving = ref(false)

function openAdd() {
  editing.value = {
    id: 0,
    name: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: false,
  }
  showEdit.value = true
}

function openEdit(a) {
  editing.value = { ...a }
  showEdit.value = true
}

function closeEdit() {
  showEdit.value = false
}

async function saveAddr() {
  if (!editing.value.name || !editing.value.phone || !editing.value.detail) {
    ElMessage.warning('请填写完整的收货信息')
    return
  }
  saving.value = true
  try {
    const body = {
      receiverName: editing.value.name,
      receiverPhone: editing.value.phone,
      province: editing.value.province || '',
      city: editing.value.city || '',
      district: editing.value.district || '',
      detail: editing.value.detail,
      isDefault: editing.value.isDefault ? 1 : 0,
    }
    if (editing.value.id) {
      await addr.update(editing.value.id, body)
      ElMessage.success('地址已更新')
    } else {
      await addr.add(body)
      ElMessage.success('地址已添加')
    }
    closeEdit()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
  saving.value = false
}

async function removeAddr(a) {
  try {
    await ElMessageBox.confirm('确定删除该地址吗？', '删除地址', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch { return }
  try {
    await addr.remove(a.id)
    ElMessage.success('地址已删除')
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  addr.fetchList()
})
</script>
