<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { recommendationsApi } from '@/api/recommendations'
import { stocksApi } from '@/api/stocks'
import { useAuthStore } from '@/stores/auth'
import { extractErrorMessage } from '@/api/client'
import { formatKRW } from '@/utils/format'
import { INVESTMENT_PERIOD_LABEL, RISK_PROFILE_LABEL } from '@/types'
import type { Recommendation } from '@/types'
import AppHeader from '@/components/layout/AppHeader.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const auth = useAuthStore()
const router = useRouter()

const rec = ref<Recommendation | null>(null)
const priceByCode = ref<Record<string, number | null>>({})
const loading = ref(true)
const error = ref('')

const profileText = computed(() =>
  auth.me
    ? `${RISK_PROFILE_LABEL[auth.me.riskProfile]} · ${INVESTMENT_PERIOD_LABEL[auth.me.investmentPeriod]}`
    : '',
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [r, s] = await Promise.all([recommendationsApi.get(), stocksApi.list()])
    rec.value = r.data
    priceByCode.value = Object.fromEntries(s.data.map((x) => [x.code, x.price]))
  } catch (e) {
    error.value = extractErrorMessage(e, '추천을 불러오지 못했어요')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function pct(score: number) {
  return Math.round(score * 100)
}
function goDetail(code: string) {
  router.push(`/stocks/${code}`)
}
</script>

<template>
  <AppHeader title="맞춤 추천" />
  <main class="rec">
    <BaseCard class="intro">
      <p class="intro__title">🎯 {{ auth.me?.nickname ?? '투자자' }} 님을 위한 추천</p>
      <span class="intro__badge">{{ profileText }}</span>
      <p class="intro__desc">투자 성향과 재무지표(PER·ROE·배당률 등)를 가중치로 점수화했어요</p>
    </BaseCard>

    <div v-if="loading" class="state">추천을 계산하는 중…</div>
    <div v-else-if="error" class="state">{{ error }}</div>
    <div v-else-if="rec && rec.items.length === 0" class="state">추천할 종목이 없어요</div>

    <ul v-else-if="rec" class="rank-list">
      <li v-for="(item, idx) in rec.items" :key="item.code">
        <BaseCard :class="['rec-card', { 'rec-card--top': idx === 0 }]" @click="goDetail(item.code)">
          <div class="rec-card__head">
            <span :class="['rec-card__rank', { 'rec-card__rank--top': idx === 0 }]">{{ idx + 1 }}</span>
            <div class="rec-card__title">
              <span class="rec-card__name">{{ item.name }}</span>
              <span class="rec-card__code">{{ item.code }}</span>
            </div>
            <span class="rec-card__price tabular">{{ formatKRW(priceByCode[item.code] ?? null) }}</span>
          </div>
          <div class="score">
            <div class="score__track">
              <div
                :class="['score__fill', { 'score__fill--top': idx === 0 }]"
                :style="{ width: `${pct(item.score)}%` }"
              />
            </div>
            <span class="score__label tabular">매칭 {{ pct(item.score) }}%</span>
          </div>
        </BaseCard>
      </li>
    </ul>

    <BaseButton v-if="!loading" variant="secondary" size="md" @click="load">추천 새로고침</BaseButton>
  </main>
</template>

<style scoped>
.rec {
  flex: 1;
  padding: var(--space-5);
  padding-bottom: 84px;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.intro {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.intro__title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.intro__badge {
  align-self: flex-start;
  padding: 5px 11px;
  background: var(--color-primary-weak);
  color: var(--color-primary);
  border-radius: var(--radius-pill);
  font-size: 12px;
  font-weight: 600;
}
.intro__desc {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}
.rank-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.rec-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  cursor: pointer;
}
.rec-card--top {
  border: 1.5px solid var(--color-primary);
}
.rec-card__head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.rec-card__rank {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--color-bg);
  color: var(--color-text-sub);
  font-size: 14px;
  font-weight: 700;
}
.rec-card__rank--top {
  background: var(--color-primary);
  color: #fff;
}
.rec-card__title {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.rec-card__name {
  font-size: 16px;
  font-weight: 700;
}
.rec-card__code {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.rec-card__price {
  font-size: 15px;
  font-weight: 600;
}
.score {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.score__track {
  flex: 1;
  height: 8px;
  background: var(--color-bg);
  border-radius: var(--radius-pill);
  overflow: hidden;
}
.score__fill {
  height: 100%;
  background: var(--color-text-tertiary);
  border-radius: var(--radius-pill);
  transition: width 0.4s ease;
}
.score__fill--top {
  background: var(--color-primary);
}
.score__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-sub);
  width: 64px;
  text-align: right;
}
.state {
  padding: var(--space-10) var(--space-2);
  text-align: center;
  color: var(--color-text-tertiary);
}
</style>
