<script setup lang="ts">
import { computed } from 'vue'
import type { ChartPoint } from '@/types'

const props = withDefaults(defineProps<{ points: ChartPoint[]; height?: number }>(), {
  height: 200,
})

const W = 320
const H = 200
const PRICE_H = 140 // 가격 영역 높이
const VOL_TOP = 156 // 거래량 영역 시작 y
const VOL_H = 44

const view = computed(() => {
  const pts = props.points
  if (pts.length < 2) return null
  const min = Math.min(...pts.map((p) => p.low))
  const max = Math.max(...pts.map((p) => p.high))
  const span = max - min || 1
  const volMax = Math.max(...pts.map((p) => p.volume), 1)
  const step = W / pts.length
  const candleW = Math.max(1, step * 0.6)

  const yPrice = (v: number) => 6 + ((max - v) / span) * (PRICE_H - 12)

  const candles = pts.map((p, i) => {
    const cx = i * step + step / 2
    const up = p.close >= p.open
    const yo = yPrice(p.open)
    const yc = yPrice(p.close)
    return {
      cx,
      up,
      wickTop: yPrice(p.high),
      wickBottom: yPrice(p.low),
      bodyY: Math.min(yo, yc),
      bodyH: Math.max(1, Math.abs(yo - yc)),
      volY: VOL_TOP + (VOL_H - (p.volume / volMax) * VOL_H),
      volH: Math.max(0.5, (p.volume / volMax) * VOL_H),
      candleW,
    }
  })

  const ma = (period: number) => {
    const closes = pts.map((p) => p.close)
    const out: string[] = []
    for (let i = 0; i < closes.length; i++) {
      if (i < period - 1) continue
      let sum = 0
      for (let j = i - period + 1; j <= i; j++) sum += closes[j]
      const cx = i * step + step / 2
      out.push(`${cx.toFixed(1)},${yPrice(sum / period).toFixed(1)}`)
    }
    return out.join(' ')
  }

  return { candles, ma5: ma(5), ma20: ma(20) }
})
</script>

<template>
  <div class="candle" :style="{ height: `${height}px` }">
    <svg v-if="view" :viewBox="`0 0 ${W} ${H}`" preserveAspectRatio="none" class="candle__svg">
      <!-- 거래량 -->
      <rect
        v-for="(c, i) in view.candles"
        :key="`v${i}`"
        :x="c.cx - c.candleW / 2"
        :y="c.volY"
        :width="c.candleW"
        :height="c.volH"
        :fill="c.up ? 'var(--color-up)' : 'var(--color-down)'"
        opacity="0.3"
      />
      <!-- 캔들 -->
      <template v-for="(c, i) in view.candles" :key="`c${i}`">
        <line
          :x1="c.cx"
          :x2="c.cx"
          :y1="c.wickTop"
          :y2="c.wickBottom"
          :stroke="c.up ? 'var(--color-up)' : 'var(--color-down)'"
          stroke-width="1"
          vector-effect="non-scaling-stroke"
        />
        <rect
          :x="c.cx - c.candleW / 2"
          :y="c.bodyY"
          :width="c.candleW"
          :height="c.bodyH"
          :fill="c.up ? 'var(--color-up)' : 'var(--color-down)'"
        />
      </template>
      <!-- 이동평균선 -->
      <polyline
        v-if="view.ma5"
        :points="view.ma5"
        fill="none"
        stroke="#f59e0b"
        stroke-width="1.5"
        vector-effect="non-scaling-stroke"
      />
      <polyline
        v-if="view.ma20"
        :points="view.ma20"
        fill="none"
        stroke="#8b5cf6"
        stroke-width="1.5"
        vector-effect="non-scaling-stroke"
      />
    </svg>
    <div v-else class="candle__empty">차트 데이터가 없어요</div>

    <div v-if="view" class="candle__legend">
      <span class="legend legend--ma5">— MA5</span>
      <span class="legend legend--ma20">— MA20</span>
    </div>
  </div>
</template>

<style scoped>
.candle {
  width: 100%;
  position: relative;
}
.candle__svg {
  width: 100%;
  height: 100%;
  display: block;
}
.candle__empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-size: 14px;
}
.candle__legend {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  gap: 8px;
  font-size: 10px;
  font-weight: 700;
}
.legend--ma5 {
  color: #f59e0b;
}
.legend--ma20 {
  color: #8b5cf6;
}
</style>
