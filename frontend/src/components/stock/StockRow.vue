<script setup lang="ts">
import { formatKRW } from '@/utils/format'
import type { Direction } from '@/utils/format'

defineProps<{
  name: string
  code: string
  price?: number | null
  direction?: Direction
  rank?: number
  meta?: string
}>()

defineEmits<{ (e: 'click'): void }>()
</script>

<template>
  <button class="row" @click="$emit('click')">
    <span v-if="rank != null" class="row__rank">{{ rank }}</span>
    <span class="row__main">
      <span class="row__name">{{ name }}</span>
      <span class="row__sub">
        <span class="row__code">{{ code }}</span>
        <span v-if="meta" class="row__meta">· {{ meta }}</span>
      </span>
    </span>
    <span :class="['row__price', 'tabular', `dir-${direction ?? 'flat'}`]">
      <span v-if="direction === 'up'" class="row__arrow">▲</span>
      <span v-else-if="direction === 'down'" class="row__arrow">▼</span>
      {{ formatKRW(price) }}
    </span>
  </button>
</template>

<style scoped>
.row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-3) var(--space-1);
  background: transparent;
  border: none;
  text-align: left;
}
.row:active {
  background: var(--color-surface-hover);
  border-radius: var(--radius-md);
}
.row__rank {
  width: 22px;
  flex-shrink: 0;
  text-align: center;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-tertiary);
}
.row__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.row__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-strong);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.row__sub {
  display: flex;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.row__price {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-strong);
  flex-shrink: 0;
}
.row__arrow {
  font-size: 10px;
}
.dir-up {
  color: var(--color-up);
}
.dir-down {
  color: var(--color-down);
}
</style>
