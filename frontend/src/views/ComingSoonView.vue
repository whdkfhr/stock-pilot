<script setup lang="ts">
import { useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
const route = useRoute()
const title = (route.meta.title as string) ?? '준비 중'
</script>

<template>
  <AppHeader :title="title" />
  <main class="coming">
    <span class="coming__icon">🚧</span>
    <p class="coming__text">{{ title }} 화면은 곧 제공돼요</p>
  </main>
</template>

<style scoped>
.coming {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding-bottom: 80px;
}
.coming__icon {
  font-size: 44px;
}
.coming__text {
  color: var(--color-text-tertiary);
  font-size: 15px;
}
</style>
