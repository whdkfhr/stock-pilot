<script setup lang="ts">
withDefaults(defineProps<{ padded?: boolean }>(), { padded: true })
</script>

<template>
  <section :class="['card', { 'card--padded': padded }]">
    <slot />
  </section>
</template>

<style scoped>
.card {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
}
.card--padded {
  padding: var(--space-5);
}
</style>
