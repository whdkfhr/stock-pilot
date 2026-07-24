<script setup lang="ts">
import { ref, watch } from 'vue'
import { formatPrice, formatChange, formatPercent, directionFromChange } from '@/utils/format'

const props = defineProps<{
  name: string
  code: string
  price?: number | null
  currency?: string
  change?: number | null
  changePercent?: number | null
  rank?: number
  meta?: string
}>()

defineEmits<{ (e: 'click'): void }>()

const dir = () => directionFromChange(props.change)

// 시세가 갱신되는 순간 방향에 따라 깜빡임(상승 빨강/하락 파랑)
const flash = ref('')
watch(
  () => props.price,
  (n, o) => {
    if (o == null || n == null || n === o) return
    flash.value = n > o ? 'flash-up' : 'flash-down'
    window.setTimeout(() => {
      flash.value = ''
    }, 700)
  },
)
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
    <span :class="['row__pricebox', flash]">
      <span class="row__price tabular">{{ formatPrice(price, currency) }}</span>
      <span v-if="change != null" :class="['row__change', 'tabular', `dir-${dir()}`]">
        {{ formatChange(change, currency) }} ({{ formatPercent(changePercent) }})
      </span>
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
.row__pricebox {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
  padding: 4px 8px;
  margin: -4px -8px;
  border-radius: var(--radius-sm);
}
.flash-up {
  animation: flash-up 0.7s ease-out;
}
.flash-down {
  animation: flash-down 0.7s ease-out;
}
@keyframes flash-up {
  0% {
    background: color-mix(in srgb, var(--color-up) 22%, transparent);
  }
  100% {
    background: transparent;
  }
}
@keyframes flash-down {
  0% {
    background: color-mix(in srgb, var(--color-down) 22%, transparent);
  }
  100% {
    background: transparent;
  }
}
.row__price {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-strong);
}
.row__change {
  font-size: 12px;
  font-weight: 600;
}
.dir-up {
  color: var(--color-up);
}
.dir-down {
  color: var(--color-down);
}
.dir-flat {
  color: var(--color-text-tertiary);
}
</style>
