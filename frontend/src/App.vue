<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

// 새로고침 시 저장된 토큰으로 사용자 정보 복구
const auth = useAuthStore()
onMounted(() => {
  if (auth.isAuthenticated && !auth.me) {
    auth.fetchMe().catch(() => auth.logout())
  }
})
</script>

<template>
  <div class="app-shell">
    <RouterView />
  </div>
</template>
