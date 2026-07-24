<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { formatPrice } from '@/utils/format'

const props = withDefaults(
  defineProps<{ value: number | null; currency?: string; duration?: number }>(),
  { duration: 450 },
)

const display = ref<number | null>(props.value)
let raf = 0

function easeOut(t: number) {
  return 1 - (1 - t) * (1 - t)
}

watch(
  () => props.value,
  (to, from) => {
    if (to == null) {
      display.value = null
      return
    }
    const start = from ?? to
    const t0 = performance.now()
    cancelAnimationFrame(raf)
    const step = (now: number) => {
      const p = Math.min(1, (now - t0) / props.duration)
      display.value = Math.round(start + (to - start) * easeOut(p))
      if (p < 1) raf = requestAnimationFrame(step)
    }
    raf = requestAnimationFrame(step)
  },
)

onUnmounted(() => cancelAnimationFrame(raf))
</script>

<template>
  <span class="tabular">{{ formatPrice(display, currency) }}</span>
</template>
