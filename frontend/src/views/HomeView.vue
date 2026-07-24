<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useLiveStocks } from '@/composables/useLiveStocks'
import { useMarketStatus, type MarketSession } from '@/composables/useMarketStatus'
import { stocksApi } from '@/api/stocks'
import { INVESTMENT_PERIOD_LABEL, RISK_PROFILE_LABEL } from '@/types'
import type { RankingItem, WatchlistItem } from '@/types'
import { formatNumber } from '@/utils/format'
import AppHeader from '@/components/layout/AppHeader.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import SectionHeader from '@/components/ui/SectionHeader.vue'
import StockRow from '@/components/stock/StockRow.vue'

const auth = useAuthStore()
const router = useRouter()
const { stocks, loading } = useLiveStocks()
const { krx, us } = useMarketStatus()

function sessionDot(s: MarketSession) {
  if (s === 'regular') return 'mkt__dot--open'
  if (s === 'closed') return 'mkt__dot--closed'
  return 'mkt__dot--ext'
}

const ranking = ref<RankingItem[]>([])
const watchlist = ref<WatchlistItem[]>([])

const search = ref('')
const marketFilter = ref<'ALL' | 'KOSPI' | 'KOSDAQ' | 'US'>('ALL')
const marketTabs = [
  { value: 'ALL', label: '전체' },
  { value: 'KOSPI', label: '코스피' },
  { value: 'KOSDAQ', label: '코스닥' },
  { value: 'US', label: '미국' },
] as const

const filteredStocks = computed(() => {
  const q = search.value.trim().toLowerCase()
  return stocks.value.filter((s) => {
    const matchQ = !q || s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q)
    const matchMarket =
      marketFilter.value === 'ALL' ||
      (marketFilter.value === 'US' ? s.market === 'NASDAQ' || s.market === 'NYSE' : s.market === marketFilter.value)
    return matchQ && matchMarket
  })
})

const profileText = computed(() =>
  auth.me
    ? `${RISK_PROFILE_LABEL[auth.me.riskProfile]} · ${INVESTMENT_PERIOD_LABEL[auth.me.investmentPeriod]}`
    : '',
)

const priceByCode = computed(() => {
  const m = new Map<string, number | null>()
  for (const s of stocks.value) m.set(s.code, s.price)
  return m
})
function priceFor(code: string) {
  return priceByCode.value.get(code) ?? null
}
const stockByCode = computed(() => {
  const m = new Map<string, (typeof stocks.value)[number]>()
  for (const s of stocks.value) m.set(s.code, s)
  return m
})
function currencyFor(code: string) {
  return stockByCode.value.get(code)?.currency ?? 'KRW'
}
function changeFor(code: string) {
  return stockByCode.value.get(code)?.change ?? null
}
function changePercentFor(code: string) {
  return stockByCode.value.get(code)?.changePercent ?? null
}

onMounted(async () => {
  try {
    ranking.value = (await stocksApi.popularRanking(5)).data
  } catch {
    /* 랭킹 비어있을 수 있음 */
  }
  try {
    watchlist.value = (await stocksApi.myWatchlist(0, 5)).data.content
  } catch {
    /* 미인증/빈 목록 */
  }
})

function goDetail(code: string) {
  router.push(`/stocks/${code}`)
}

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <AppHeader>
    <template #action>
      <button class="logout" @click="logout">로그아웃</button>
    </template>
  </AppHeader>

  <main class="home">
    <!-- 인사 -->
    <BaseCard class="hero" :padded="true">
      <p class="hero__hi">안녕하세요, {{ auth.me?.nickname ?? '투자자' }} 님 👋</p>
      <span class="hero__badge">{{ profileText }}</span>
    </BaseCard>

    <!-- 실시간 시세 -->
    <section>
      <SectionHeader title="실시간 시세" live>
        <template #action>
          <span class="mkt">
            <span :class="['mkt__dot', sessionDot(krx.session)]" />국내 {{ krx.label }}
          </span>
          <span class="mkt">
            <span :class="['mkt__dot', sessionDot(us.session)]" />미국 {{ us.label }}
          </span>
        </template>
      </SectionHeader>

      <div class="search">
        <span class="search__icon">🔍</span>
        <input v-model="search" class="search__input" placeholder="종목명 · 코드 검색 (예: 삼성, AAPL)" />
        <button v-if="search" class="search__clear" @click="search = ''">✕</button>
      </div>

      <div class="chips">
        <button
          v-for="t in marketTabs"
          :key="t.value"
          :class="['chip', { 'chip--on': marketFilter === t.value }]"
          @click="marketFilter = t.value"
        >
          {{ t.label }}
        </button>
      </div>

      <BaseCard :padded="false" class="list">
        <div v-if="loading && stocks.length === 0" class="state">시세를 불러오는 중…</div>
        <div v-else-if="filteredStocks.length === 0" class="state">검색 결과가 없어요</div>
        <ul v-else>
          <li v-for="s in filteredStocks" :key="s.code" class="list__item">
            <StockRow
              :name="s.name"
              :code="s.code"
              :price="s.price"
              :currency="s.currency"
              :change="s.change"
              :change-percent="s.changePercent"
              @click="goDetail(s.code)"
            />
          </li>
        </ul>
      </BaseCard>
    </section>

    <!-- 인기 랭킹 -->
    <section>
      <SectionHeader title="🔥 지금 인기" />
      <BaseCard :padded="false" class="list">
        <div v-if="ranking.length === 0" class="state">아직 조회 기록이 없어요</div>
        <ul v-else>
          <li v-for="item in ranking" :key="item.code" class="list__item">
            <StockRow
              :rank="item.rank"
              :name="item.name"
              :code="item.code"
              :price="priceFor(item.code)"
              :currency="currencyFor(item.code)"
              :change="changeFor(item.code)"
              :change-percent="changePercentFor(item.code)"
              :meta="`조회 ${formatNumber(item.viewCount)}`"
              @click="goDetail(item.code)"
            />
          </li>
        </ul>
      </BaseCard>
    </section>

    <!-- 관심종목 -->
    <section>
      <SectionHeader title="⭐ 관심종목" />
      <BaseCard :padded="false" class="list">
        <div v-if="watchlist.length === 0" class="state state--hint">
          아직 담은 종목이 없어요<br /><small>종목 상세에서 관심종목을 추가할 수 있어요</small>
        </div>
        <ul v-else>
          <li v-for="w in watchlist" :key="w.watchlistId" class="list__item">
            <StockRow
              :name="w.stockName"
              :code="w.stockCode"
              :price="priceFor(w.stockCode)"
              :currency="currencyFor(w.stockCode)"
              :change="changeFor(w.stockCode)"
              :change-percent="changePercentFor(w.stockCode)"
              @click="goDetail(w.stockCode)"
            />
          </li>
        </ul>
      </BaseCard>
    </section>
  </main>
</template>

<style scoped>
.logout {
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  font-size: 14px;
  font-weight: 600;
}
.home {
  flex: 1;
  padding: var(--space-5);
  padding-bottom: 84px;
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}
.hero {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.hero__hi {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text-strong);
}
.hero__badge {
  align-self: flex-start;
  padding: 6px 12px;
  background: var(--color-primary-weak);
  color: var(--color-primary);
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 600;
}
.mkt {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-tertiary);
}
.mkt:first-of-type {
  margin-left: auto;
}
.mkt__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.mkt__dot--open {
  background: var(--color-success);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-success) 25%, transparent);
}
.mkt__dot--ext {
  background: var(--color-warning);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-warning) 25%, transparent);
}
.mkt__dot--closed {
  background: var(--color-text-tertiary);
  opacity: 0.5;
}
.search {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  height: 46px;
  margin-bottom: var(--space-3);
}
.search__icon {
  font-size: 14px;
  opacity: 0.6;
}
.search__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: var(--color-text-strong);
}
.search__input::placeholder {
  color: var(--color-text-tertiary);
}
.search__clear {
  border: none;
  background: var(--color-bg);
  color: var(--color-text-tertiary);
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 11px;
}
.chips {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}
.chip {
  padding: 7px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-sub);
}
.chip--on {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
.list {
  padding: var(--space-2) var(--space-4);
}
.list__item + .list__item {
  border-top: 1px solid var(--color-divider);
}
.state {
  padding: var(--space-6) var(--space-2);
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 14px;
}
.state--hint small {
  display: inline-block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
</style>
