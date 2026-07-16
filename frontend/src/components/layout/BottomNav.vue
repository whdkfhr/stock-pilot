<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { name: '홈', icon: '🏠', to: '/' },
  { name: '추천', icon: '🎯', to: '/recommend' },
  { name: '알림', icon: '🔔', to: '/notifications' },
  { name: '마이', icon: '👤', to: '/my' },
]

function isActive(to: string) {
  return to === '/' ? route.path === '/' : route.path.startsWith(to)
}
</script>

<template>
  <nav class="bottom-nav">
    <RouterLink
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      :class="['tab', { 'tab--active': isActive(tab.to) }]"
    >
      <span class="tab__icon">{{ tab.icon }}</span>
      <span class="tab__label">{{ tab.name }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: var(--app-max-width);
  height: 60px;
  display: flex;
  background: var(--color-surface);
  border-top: 1px solid var(--color-divider);
  z-index: 20;
}
.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: var(--color-text-tertiary);
}
.tab__icon {
  font-size: 20px;
  filter: grayscale(1);
  opacity: 0.55;
  transition: all 0.15s ease;
}
.tab__label {
  font-size: 11px;
  font-weight: 600;
}
.tab--active {
  color: var(--color-primary);
}
.tab--active .tab__icon {
  filter: none;
  opacity: 1;
}
</style>
