<script setup lang="ts">
defineProps<{ title?: string }>()
</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <span class="app-header__logo">🛩️</span>
      <span class="app-header__name">{{ title ?? 'StockPilot' }}</span>
    </div>
    <slot name="action" />
  </header>
</template>

<style scoped>
.app-header {
  height: 56px;
  padding: 0 var(--space-5);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-surface);
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 1px solid var(--color-divider);
}
.app-header__brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.app-header__logo {
  font-size: 20px;
}
.app-header__name {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-strong);
  letter-spacing: -0.02em;
}
</style>
