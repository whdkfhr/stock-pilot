<script setup lang="ts" generic="T extends string">
defineProps<{
  label?: string
  options: { value: T; label: string; hint?: string }[]
}>()

const model = defineModel<T>()
</script>

<template>
  <div class="seg">
    <span v-if="label" class="seg__label">{{ label }}</span>
    <div class="seg__group">
      <button
        v-for="opt in options"
        :key="opt.value"
        type="button"
        :class="['seg__item', { 'seg__item--active': model === opt.value }]"
        @click="model = opt.value"
      >
        <span class="seg__item-label">{{ opt.label }}</span>
        <span v-if="opt.hint" class="seg__item-hint">{{ opt.hint }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.seg {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.seg__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-sub);
  padding-left: 2px;
}
.seg__group {
  display: flex;
  gap: var(--space-2);
}
.seg__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 14px 8px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  transition: all 0.15s ease;
}
.seg__item--active {
  border-color: var(--color-primary);
  background: var(--color-primary-weak);
}
.seg__item-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}
.seg__item--active .seg__item-label {
  color: var(--color-primary);
}
.seg__item-hint {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
