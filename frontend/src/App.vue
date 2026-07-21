<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'
import BottomNav from '@/components/layout/BottomNav.vue'

// 새로고침 시 저장된 토큰으로 사용자 정보 복구
const auth = useAuthStore()
const notifications = useNotificationStore()
const route = useRoute()

let pollTimer: number | undefined
onMounted(() => {
  if (auth.isAuthenticated && !auth.me) {
    auth.fetchMe().catch(() => auth.logout())
  }
  // 알림 배지: 로그인 상태면 주기적으로 미읽음 조회
  if (auth.isAuthenticated) notifications.fetch().catch(() => {})
  pollTimer = window.setInterval(() => {
    if (auth.isAuthenticated) notifications.fetch().catch(() => {})
  }, 30_000)
})
onUnmounted(() => {
  if (pollTimer) window.clearInterval(pollTimer)
})
</script>

<template>
  <div class="app-shell">
    <RouterView :key="route.fullPath" />
    <BottomNav v-if="route.meta.tab" />
  </div>
</template>
