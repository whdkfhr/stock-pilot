<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { stocksApi } from '@/api/stocks'
import { extractErrorMessage } from '@/api/client'
import { formatKRW, formatNumber } from '@/utils/format'
import type { StockDetail } from '@/types'
import BaseCard from '@/components/ui/BaseCard.vue'
import Sparkline from '@/components/stock/Sparkline.vue'

const route = useRoute()
const router = useRouter()
const code = route.params.code as string

const detail = ref<StockDetail | null>(null)
const prices = ref<number[]>([])
const likeCount = ref(0)
const liked = ref(false)
const watched = ref(false)
const loading = ref(true)
const error = ref('')
const busyLike = ref(false)
const busyWatch = ref(false)

const currentPrice = computed(
  () => detail.value?.price ?? (prices.value.length ? prices.value[prices.value.length - 1] : null),
)

const change = computed(() => {
  if (prices.value.length < 2 || currentPrice.value == null) return null
  const first = prices.value[0]
  const delta = currentPrice.value - first
  const pct = first ? (delta / first) * 100 : 0
  return { delta, pct, up: delta >= 0 }
})

const metrics = computed(() => {
  const d = detail.value
  if (!d) return []
  return [
    { label: 'PER', value: d.per.toFixed(1) },
    { label: 'PBR', value: d.pbr.toFixed(1) },
    { label: 'ROE', value: `${d.roe.toFixed(1)}%` },
    { label: '배당률', value: `${d.dividendYield.toFixed(1)}%` },
  ]
})

onMounted(async () => {
  stocksApi.recordView(code).catch(() => {})
  try {
    const [d, h, l] = await Promise.all([
      stocksApi.detail(code),
      stocksApi.history(code),
      stocksApi.likes(code),
    ])
    detail.value = d.data
    prices.value = h.data.map((p) => p.price).reverse() // 최신순 → 시간순
    likeCount.value = l.data.likeCount
    try {
      const wl = await stocksApi.myWatchlist(0, 100)
      watched.value = wl.data.content.some((w) => w.stockCode === code)
    } catch {
      /* 미인증 등 */
    }
  } catch (e) {
    error.value = extractErrorMessage(e, '종목 정보를 불러오지 못했어요')
  } finally {
    loading.value = false
  }
})

async function toggleLike() {
  if (liked.value || busyLike.value) return
  busyLike.value = true
  try {
    const { data } = await stocksApi.like(code)
    likeCount.value = data.likeCount
    liked.value = true
  } catch (e) {
    error.value = extractErrorMessage(e)
  } finally {
    busyLike.value = false
  }
}

async function toggleWatch() {
  if (!detail.value || busyWatch.value) return
  busyWatch.value = true
  const id = detail.value.id
  try {
    if (watched.value) {
      await stocksApi.unwatch(id)
      watched.value = false
      detail.value.watchCount = Math.max(0, detail.value.watchCount - 1)
    } else {
      await stocksApi.watch(id)
      watched.value = true
      detail.value.watchCount += 1
    }
  } catch (e) {
    error.value = extractErrorMessage(e)
  } finally {
    busyWatch.value = false
  }
}
</script>

<template>
  <header class="detail-header">
    <button class="back" @click="router.back()">←</button>
    <span class="detail-header__title">{{ detail?.name ?? '종목' }}</span>
    <span class="detail-header__spacer" />
  </header>

  <main class="detail">
    <div v-if="loading" class="state">불러오는 중…</div>
    <div v-else-if="error" class="state">{{ error }}</div>

    <template v-else-if="detail">
      <!-- 현재가 -->
      <section class="price-block">
        <p class="price-block__code">{{ detail.code }}</p>
        <h1 class="price-block__price tabular">{{ formatKRW(currentPrice) }}</h1>
        <p v-if="change" :class="['price-block__change', change.up ? 'dir-up' : 'dir-down']">
          <span>{{ change.up ? '▲' : '▼' }}</span>
          <span class="tabular">{{ formatNumber(Math.abs(change.delta)) }}원</span>
          <span class="tabular">({{ change.pct >= 0 ? '+' : '' }}{{ change.pct.toFixed(2) }}%)</span>
        </p>
        <p v-else class="price-block__change dir-flat">데이터 수집 중</p>
      </section>

      <!-- 미니차트 -->
      <BaseCard>
        <p class="card-label">최근 시세 흐름</p>
        <Sparkline :values="prices" />
      </BaseCard>

      <!-- 투자지표 -->
      <section>
        <p class="section-label">투자지표</p>
        <div class="metrics">
          <BaseCard v-for="m in metrics" :key="m.label" class="metric">
            <span class="metric__label">{{ m.label }}</span>
            <span class="metric__value tabular">{{ m.value }}</span>
          </BaseCard>
        </div>
      </section>

      <!-- 인기 지표 -->
      <BaseCard class="stat-row">
        <div class="stat">
          <span class="stat__label">❤️ 좋아요</span>
          <span class="stat__value tabular">{{ formatNumber(likeCount) }}</span>
        </div>
        <div class="stat__divider" />
        <div class="stat">
          <span class="stat__label">⭐ 관심</span>
          <span class="stat__value tabular">{{ formatNumber(detail.watchCount) }}</span>
        </div>
      </BaseCard>
    </template>
  </main>

  <!-- 액션 바 -->
  <div v-if="detail && !loading" class="action-bar">
    <button
      :class="['action', { 'action--on': liked }]"
      :disabled="busyLike"
      @click="toggleLike"
    >
      {{ liked ? '❤️' : '🤍' }} 좋아요
    </button>
    <button
      :class="['action', 'action--primary', { 'action--on': watched }]"
      :disabled="busyWatch"
      @click="toggleWatch"
    >
      {{ watched ? '⭐ 관심종목 담김' : '☆ 관심종목 담기' }}
    </button>
  </div>
</template>

<style scoped>
.detail-header {
  height: 56px;
  padding: 0 var(--space-4);
  display: flex;
  align-items: center;
  background: var(--color-surface);
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 1px solid var(--color-divider);
}
.back {
  border: none;
  background: transparent;
  font-size: 22px;
  color: var(--color-text-strong);
  width: 32px;
}
.detail-header__title {
  flex: 1;
  text-align: center;
  font-size: 17px;
  font-weight: 700;
}
.detail-header__spacer {
  width: 32px;
}
.detail {
  flex: 1;
  padding: var(--space-5);
  padding-bottom: 96px;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.price-block__code {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.price-block__price {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -0.03em;
  margin-top: 4px;
}
.price-block__change {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 6px;
  font-size: 15px;
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
.card-label,
.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-sub);
  margin-bottom: var(--space-3);
}
.section-label {
  padding-left: 2px;
}
.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
}
.metric {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.metric__label {
  font-size: 14px;
  color: var(--color-text-sub);
}
.metric__value {
  font-size: 17px;
  font-weight: 700;
}
.stat-row {
  display: flex;
  align-items: center;
}
.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.stat__label {
  font-size: 13px;
  color: var(--color-text-sub);
}
.stat__value {
  font-size: 18px;
  font-weight: 700;
}
.stat__divider {
  width: 1px;
  align-self: stretch;
  background: var(--color-divider);
}
.action-bar {
  position: sticky;
  bottom: 0;
  display: flex;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-5) var(--space-5);
  background: linear-gradient(to top, var(--color-bg) 70%, transparent);
}
.action {
  height: 52px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}
.action:disabled {
  opacity: 0.5;
}
.action--on {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-weak);
}
.action--primary {
  flex: 1;
}
.state {
  padding: var(--space-10) var(--space-2);
  text-align: center;
  color: var(--color-text-tertiary);
}
</style>
