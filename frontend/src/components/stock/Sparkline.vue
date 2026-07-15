<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ values: number[]; height?: number }>(), { height: 96 })

const W = 300
const H = 100

const up = computed(() => {
  const v = props.values
  return v.length < 2 ? true : v[v.length - 1] >= v[0]
})

const points = computed(() => {
  const v = props.values
  if (v.length < 2) return ''
  const min = Math.min(...v)
  const max = Math.max(...v)
  const span = max - min || 1
  const stepX = W / (v.length - 1)
  return v
    .map((val, i) => {
      const x = i * stepX
      const y = H - 8 - ((val - min) / span) * (H - 16)
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
})

const areaPath = computed(() => {
  if (!points.value) return ''
  return `M0,${H} L${points.value.split(' ').join(' L')} L${W},${H} Z`
})

const color = computed(() => (up.value ? 'var(--color-up)' : 'var(--color-down)'))
</script>

<template>
  <div class="spark" :style="{ height: `${height}px` }">
    <svg :viewBox="`0 0 ${W} ${H}`" preserveAspectRatio="none" class="spark__svg">
      <defs>
        <linearGradient :id="`g-${up}`" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" :stop-color="color" stop-opacity="0.18" />
          <stop offset="100%" :stop-color="color" stop-opacity="0" />
        </linearGradient>
      </defs>
      <path v-if="areaPath" :d="areaPath" :fill="`url(#g-${up})`" />
      <polyline
        v-if="points"
        :points="points"
        fill="none"
        :stroke="color"
        stroke-width="2.5"
        stroke-linejoin="round"
        stroke-linecap="round"
        vector-effect="non-scaling-stroke"
      />
      <text v-if="!points" x="150" y="55" text-anchor="middle" class="spark__empty">
        시세 데이터가 쌓이는 중…
      </text>
    </svg>
  </div>
</template>

<style scoped>
.spark {
  width: 100%;
}
.spark__svg {
  width: 100%;
  height: 100%;
  display: block;
}
.spark__empty {
  fill: var(--color-text-tertiary);
  font-size: 13px;
}
</style>
