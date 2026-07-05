<template>
  <div v-if="!authStore.isLoggedIn" class="min-h-screen">
    <router-view />
  </div>
  <div v-else class="min-h-screen bg-gray-50">
    <AdminSidebar />
    <div class="pl-60">
      <AdminHeader />
      <main class="p-6">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import AdminSidebar from '@/components/AdminSidebar.vue'
import AdminHeader from '@/components/AdminHeader.vue'

const authStore = useAuthStore()
</script>
