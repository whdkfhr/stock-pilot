<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { stocksApi } from '@/api/stocks'
import { extractErrorMessage } from '@/api/client'
import {
  INVESTMENT_PERIOD_LABEL,
  RISK_PROFILE_LABEL,
  type InvestmentPeriod,
  type RiskProfile,
  type WatchlistItem,
} from '@/types'
import { formatPrice } from '@/utils/format'
import AppHeader from '@/components/layout/AppHeader.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseSegmented from '@/components/ui/BaseSegmented.vue'

const auth = useAuthStore()
const router = useRouter()

const editing = ref(false)
const risk = ref<RiskProfile>('STABLE')
const period = ref<InvestmentPeriod>('LONG_TERM')
const saving = ref(false)
const editMsg = ref('')

const watchlist = ref<WatchlistItem[]>([])
const priceByCode = ref<Record<string, { price: number | null; currency: string }>>({})

function startEdit() {
  if (!auth.me) return
  risk.value = auth.me.riskProfile
  period.value = auth.me.investmentPeriod
  editMsg.value = ''
  editing.value = true
}

async function saveProfile() {
  saving.value = true
  editMsg.value = ''
  try {
    await auth.updateProfile({ riskProfile: risk.value, investmentPeriod: period.value })
    editing.value = false
  } catch (e) {
    editMsg.value = extractErrorMessage(e, '변경에 실패했어요')
  } finally {
    saving.value = false
  }
}

async function loadWatchlist() {
  try {
    watchlist.value = (await stocksApi.myWatchlist(0, 100)).data.content
  } catch {
    /* ignore */
  }
}

async function unwatch(item: WatchlistItem) {
  await stocksApi.unwatch(item.stockId).catch(() => {})
  watchlist.value = watchlist.value.filter((w) => w.watchlistId !== item.watchlistId)
}

onMounted(async () => {
  loadWatchlist()
  try {
    const { data } = await stocksApi.list()
    priceByCode.value = Object.fromEntries(data.map((s) => [s.code, { price: s.price, currency: s.currency }]))
  } catch {
    /* ignore */
  }
})

function logout() {
  auth.logout()
  router.replace('/login')
}

const riskOptions = [
  { value: 'AGGRESSIVE' as RiskProfile, label: '공격형' },
  { value: 'STABLE' as RiskProfile, label: '안정형' },
  { value: 'DIVIDEND' as RiskProfile, label: '배당형' },
]
const periodOptions = [
  { value: 'SHORT_TERM' as InvestmentPeriod, label: '단기' },
  { value: 'LONG_TERM' as InvestmentPeriod, label: '장기' },
]
</script>

<template>
  <AppHeader title="마이" />
  <main class="my">
    <!-- 프로필 -->
    <BaseCard class="profile">
      <div class="profile__top">
        <div class="profile__avatar">{{ (auth.me?.nickname ?? '유').charAt(0) }}</div>
        <div class="profile__info">
          <p class="profile__name">{{ auth.me?.nickname }}</p>
          <p class="profile__email">{{ auth.me?.email }}</p>
        </div>
      </div>

      <template v-if="!editing">
        <div class="profile__badges" v-if="auth.me">
          <span class="badge">{{ RISK_PROFILE_LABEL[auth.me.riskProfile] }}</span>
          <span class="badge">{{ INVESTMENT_PERIOD_LABEL[auth.me.investmentPeriod] }}</span>
        </div>
        <BaseButton variant="secondary" size="md" @click="startEdit">투자 성향 변경</BaseButton>
      </template>

      <template v-else>
        <BaseSegmented v-model="risk" label="투자 성향" :options="riskOptions" />
        <BaseSegmented v-model="period" label="투자 기간" :options="periodOptions" />
        <p v-if="editMsg" class="edit-msg">{{ editMsg }}</p>
        <div class="edit-actions">
          <BaseButton variant="ghost" size="md" @click="editing = false">취소</BaseButton>
          <BaseButton size="md" :loading="saving" @click="saveProfile">저장</BaseButton>
        </div>
      </template>
    </BaseCard>

    <!-- 관심종목 -->
    <section>
      <p class="section-label">관심종목 {{ watchlist.length ? `(${watchlist.length})` : '' }}</p>
      <BaseCard :padded="false" class="wl">
        <div v-if="watchlist.length === 0" class="state">담은 종목이 없어요</div>
        <ul v-else>
          <li v-for="w in watchlist" :key="w.watchlistId" class="wl__item">
            <div class="wl__main" @click="router.push(`/stocks/${w.stockCode}`)">
              <span class="wl__name">{{ w.stockName }}</span>
              <span class="wl__code">{{ w.stockCode }}</span>
            </div>
            <span class="wl__price tabular">{{
              formatPrice(priceByCode[w.stockCode]?.price ?? null, priceByCode[w.stockCode]?.currency)
            }}</span>
            <button class="wl__del" @click="unwatch(w)">✕</button>
          </li>
        </ul>
      </BaseCard>
    </section>

    <button class="logout" @click="logout">로그아웃</button>
  </main>
</template>

<style scoped>
.my {
  flex: 1;
  padding: var(--space-5);
  padding-bottom: 84px;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.profile {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.profile__top {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.profile__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
}
.profile__name {
  font-size: 17px;
  font-weight: 700;
}
.profile__email {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.profile__badges {
  display: flex;
  gap: var(--space-2);
}
.badge {
  padding: 6px 12px;
  background: var(--color-primary-weak);
  color: var(--color-primary);
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 600;
}
.edit-actions {
  display: flex;
  gap: var(--space-2);
}
.edit-msg {
  font-size: 13px;
  color: var(--color-danger);
}
.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-sub);
  padding-left: 2px;
  margin-bottom: var(--space-3);
}
.wl {
  padding: var(--space-1) var(--space-4);
}
.wl__item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) 0;
}
.wl__item + .wl__item {
  border-top: 1px solid var(--color-divider);
}
.wl__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
}
.wl__name {
  font-size: 15px;
  font-weight: 600;
}
.wl__code {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.wl__price {
  font-size: 14px;
  font-weight: 600;
}
.wl__del {
  border: none;
  background: var(--color-bg);
  color: var(--color-text-tertiary);
  width: 26px;
  height: 26px;
  border-radius: 50%;
  font-size: 12px;
  flex-shrink: 0;
}
.state {
  padding: var(--space-8) var(--space-2);
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 14px;
}
.logout {
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-sub);
  height: 50px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
}
</style>
