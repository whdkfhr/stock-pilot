<script setup lang="ts">
defineProps<{ title: string; live?: boolean }>()
</script>

<template>
  <div class="sh">
    <h2 class="sh__title">{{ title }}</h2>
    <span v-if="live" class="sh__live"><span class="sh__dot" />LIVE</span>
    <slot name="action" />
  </div>
</template>

<style scoped>
.sh {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 0 2px var(--space-1);
}
.sh__title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-strong);
}
.sh__live {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-up);
}
.sh__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-up);
  animation: pulse 1.2s ease-in-out infinite;
}
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}
</style>
