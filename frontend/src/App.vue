<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BottomNav from '@/components/layout/BottomNav.vue'

// 새로고침 시 저장된 토큰으로 사용자 정보 복구
const auth = useAuthStore()
const route = useRoute()
onMounted(() => {
  if (auth.isAuthenticated && !auth.me) {
    auth.fetchMe().catch(() => auth.logout())
  }
})
</script>

<template>
  <div class="app-shell">
    <RouterView :key="route.fullPath" />
    <BottomNav v-if="route.meta.tab" />
  </div>
</template>
