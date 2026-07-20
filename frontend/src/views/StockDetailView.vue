<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { stocksApi } from '@/api/stocks'
import { extractErrorMessage } from '@/api/client'
import { formatPrice, formatChange, formatPercent, formatNumber, directionFromChange } from '@/utils/format'
import type { ChartPoint, QuoteResponse, StockDetail, TradingTrend } from '@/types'
import BaseCard from '@/components/ui/BaseCard.vue'
import CandleChart from '@/components/stock/CandleChart.vue'

const route = useRoute()
const router = useRouter()
const code = route.params.code as string

const detail = ref<StockDetail | null>(null)
const likeCount = ref(0)
const liked = ref(false)
const watched = ref(false)
const loading = ref(true)
const error = ref('')
const busyLike = ref(false)
const busyWatch = ref(false)

// 차트
const periods = [
  { value: '1D', label: '1일' },
  { value: '1W', label: '1주' },
  { value: '1M', label: '1달' },
]
const period = ref('1D')
const chartPoints = ref<ChartPoint[]>([])
const chartLoading = ref(false)

// 시세 요약 + 투자자 매매동향
const quote = ref<QuoteResponse | null>(null)
const trend = ref<TradingTrend | null>(null)

const changeDir = computed(() => directionFromChange(detail.value?.change ?? null))

// 52주 최고/최저 대비 현재가 위치(0~1)
const week52Pos = computed(() => {
  const q = quote.value
  const price = detail.value?.price
  if (!q || q.week52High == null || q.week52Low == null || price == null) return null
  const span = q.week52High - q.week52Low
  if (span <= 0) return null
  return Math.min(1, Math.max(0, (price - q.week52Low) / span))
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

async function loadChart() {
  chartLoading.value = true
  try {
    const { data } = await stocksApi.chart(code, period.value)
    chartPoints.value = data.points
  } catch {
    chartPoints.value = []
  } finally {
    chartLoading.value = false
  }
}

function selectPeriod(p: string) {
  if (period.value === p) return
  period.value = p
  loadChart()
}

onMounted(async () => {
  stocksApi.recordView(code).catch(() => {})
  try {
    const [d, ls] = await Promise.all([stocksApi.detail(code), stocksApi.likeStatus(code)])
    detail.value = d.data
    liked.value = ls.data.liked
    likeCount.value = ls.data.likeCount
    loadChart()
    stocksApi.quote(code).then((r) => (quote.value = r.data)).catch(() => {})
    stocksApi.tradingTrend(code).then((r) => (trend.value = r.data)).catch(() => {})
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
  if (busyLike.value) return
  busyLike.value = true
  try {
    const { data } = liked.value ? await stocksApi.unlike(code) : await stocksApi.like(code)
    likeCount.value = data.likeCount
    liked.value = !liked.value
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
        <p class="price-block__code">{{ detail.code }} · {{ detail.market }}</p>
        <h1 class="price-block__price tabular">{{ formatPrice(detail.price, detail.currency) }}</h1>
        <p v-if="detail.change != null" :class="['price-block__change', `dir-${changeDir}`]">
          <span>{{ detail.change > 0 ? '▲' : detail.change < 0 ? '▼' : '' }}</span>
          <span class="tabular">{{ formatChange(detail.change, detail.currency).replace(/^[+-]/, '') }}</span>
          <span class="tabular">({{ formatPercent(detail.changePercent) }})</span>
          <span class="price-block__prev">전일 대비</span>
        </p>
        <p v-else class="price-block__change dir-flat">데이터 수집 중</p>
      </section>

      <!-- 기간별 차트 -->
      <BaseCard>
        <div class="chart-head">
          <p class="card-label">시세 차트</p>
          <div class="pchips">
            <button
              v-for="p in periods"
              :key="p.value"
              :class="['pchip', { 'pchip--on': period === p.value }]"
              @click="selectPeriod(p.value)"
            >
              {{ p.label }}
            </button>
          </div>
        </div>
        <div v-if="chartLoading && chartPoints.length === 0" class="chart-state">차트 불러오는 중…</div>
        <CandleChart v-else :points="chartPoints" :height="200" />
      </BaseCard>

      <!-- 오늘의 시세 -->
      <BaseCard v-if="quote">
        <p class="card-label">오늘의 시세</p>
        <div class="quote-grid">
          <div class="qitem">
            <span class="qitem__label">고가</span>
            <span class="qitem__value tabular dir-up">{{ formatPrice(quote.dayHigh, detail.currency) }}</span>
          </div>
          <div class="qitem">
            <span class="qitem__label">저가</span>
            <span class="qitem__value tabular dir-down">{{ formatPrice(quote.dayLow, detail.currency) }}</span>
          </div>
          <div class="qitem">
            <span class="qitem__label">거래량</span>
            <span class="qitem__value tabular">{{ quote.volume != null ? formatNumber(quote.volume) : '—' }}</span>
          </div>
        </div>
      </BaseCard>

      <!-- 52주 최고/최저 -->
      <BaseCard v-if="week52Pos != null && quote">
        <p class="card-label">52주 최고 · 최저</p>
        <div class="gauge">
          <div class="gauge__bar">
            <div class="gauge__marker" :style="{ left: `${week52Pos * 100}%` }" />
          </div>
          <div class="gauge__ends">
            <span class="tabular dir-down">{{ formatPrice(quote.week52Low, detail.currency) }}</span>
            <span class="gauge__cur tabular">현재 {{ formatPrice(detail.price, detail.currency) }}</span>
            <span class="tabular dir-up">{{ formatPrice(quote.week52High, detail.currency) }}</span>
          </div>
        </div>
      </BaseCard>

      <!-- 투자자 매매동향 -->
      <BaseCard v-if="trend && trend.sample && trend.flows.length">
        <div class="chart-head">
          <p class="card-label">투자자 매매동향 (당일 순매수)</p>
          <span class="sample-badge">샘플</span>
        </div>
        <ul class="flows">
          <li v-for="f in trend.flows" :key="f.investor" class="flow">
            <span class="flow__label">{{ f.investor }}</span>
            <span
              :class="['flow__value', 'tabular', f.netBuy >= 0 ? 'dir-up' : 'dir-down']"
            >
              {{ f.netBuy > 0 ? '+' : '' }}{{ formatNumber(f.netBuy) }} {{ trend.unit }}
            </span>
          </li>
        </ul>
        <p class="flow__note">※ 개인/외국인/기관 수급은 데모 샘플이며, KIS(한국투자증권) 연동 시 실데이터로 교체돼요.</p>
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

      <!-- 종목 정보 -->
      <BaseCard>
        <p class="card-label">종목 정보</p>
        <ul class="info">
          <li class="info__row"><span class="info__k">시장</span><span class="info__v">{{ detail.market }}</span></li>
          <li v-if="quote?.exchange" class="info__row">
            <span class="info__k">거래소</span><span class="info__v">{{ quote.exchange }}</span>
          </li>
          <li v-if="quote?.name" class="info__row">
            <span class="info__k">영문명</span><span class="info__v">{{ quote.name }}</span>
          </li>
          <li class="info__row">
            <span class="info__k">배당수익률</span><span class="info__v tabular">{{ detail.dividendYield.toFixed(2) }}%</span>
          </li>
        </ul>
      </BaseCard>

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
    <button :class="['action', { 'action--on': liked }]" :disabled="busyLike" @click="toggleLike">
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
.price-block__prev {
  color: var(--color-text-tertiary);
  font-weight: 500;
  font-size: 12px;
  margin-left: 2px;
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
.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);
}
.card-label,
.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-sub);
}
.section-label {
  padding-left: 2px;
  margin-bottom: var(--space-3);
}
.pchips {
  display: flex;
  gap: 6px;
}
.pchip {
  padding: 5px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-sub);
}
.pchip--on {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
.chart-state {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  font-size: 14px;
}
.sample-badge {
  padding: 3px 8px;
  background: var(--color-bg);
  color: var(--color-text-tertiary);
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 700;
}
.flows {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.flow {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.flow__label {
  font-size: 14px;
  color: var(--color-text-sub);
}
.flow__value {
  font-size: 15px;
  font-weight: 700;
}
.flow__note {
  margin-top: var(--space-3);
  font-size: 11px;
  color: var(--color-text-tertiary);
  line-height: 1.4;
}
.quote-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: var(--space-3);
}
.qitem {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.qitem__label {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.qitem__value {
  font-size: 15px;
  font-weight: 700;
}
.gauge {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.gauge__bar {
  position: relative;
  height: 8px;
  border-radius: var(--radius-pill);
  background: linear-gradient(to right, var(--color-down), var(--color-text-tertiary), var(--color-up));
  opacity: 0.9;
}
.gauge__marker {
  position: absolute;
  top: 50%;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--color-surface);
  border: 3px solid var(--color-text-strong);
  transform: translate(-50%, -50%);
}
.gauge__ends {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
}
.gauge__cur {
  color: var(--color-text-strong);
  font-weight: 700;
}
.info {
  display: flex;
  flex-direction: column;
}
.info__row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  font-size: 14px;
}
.info__row + .info__row {
  border-top: 1px solid var(--color-divider);
}
.info__k {
  color: var(--color-text-sub);
}
.info__v {
  font-weight: 600;
  color: var(--color-text-strong);
  max-width: 60%;
  text-align: right;
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
