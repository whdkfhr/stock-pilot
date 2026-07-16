<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useLiveStocks } from '@/composables/useLiveStocks'
import { stocksApi } from '@/api/stocks'
import { INVESTMENT_PERIOD_LABEL, RISK_PROFILE_LABEL } from '@/types'
import type { RankingItem, WatchlistItem } from '@/types'
import type { Direction } from '@/utils/format'
import { formatNumber } from '@/utils/format'
import AppHeader from '@/components/layout/AppHeader.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import SectionHeader from '@/components/ui/SectionHeader.vue'
import StockRow from '@/components/stock/StockRow.vue'

const auth = useAuthStore()
const router = useRouter()
const { stocks, directions, loading } = useLiveStocks()

const ranking = ref<RankingItem[]>([])
const watchlist = ref<WatchlistItem[]>([])

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
function dirFor(code: string): Direction {
  return directions.value[code] ?? 'flat'
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
      <SectionHeader title="실시간 시세" live />
      <BaseCard :padded="false" class="list">
        <div v-if="loading && stocks.length === 0" class="state">시세를 불러오는 중…</div>
        <div v-else-if="stocks.length === 0" class="state">등록된 종목이 없어요</div>
        <ul v-else>
          <li v-for="s in stocks" :key="s.code" class="list__item">
            <StockRow :name="s.name" :code="s.code" :price="s.price" :direction="dirFor(s.code)" @click="goDetail(s.code)" />
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
              :direction="dirFor(item.code)"
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
              :direction="dirFor(w.stockCode)"
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
