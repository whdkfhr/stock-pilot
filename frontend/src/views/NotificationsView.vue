<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notifications'
import { alertsApi } from '@/api/notifications'
import { stocksApi } from '@/api/stocks'
import { formatPrice } from '@/utils/format'
import { ALERT_DIRECTION_LABEL } from '@/types'
import type { Alert, StockSummary } from '@/types'
import AppHeader from '@/components/layout/AppHeader.vue'
import BaseCard from '@/components/ui/BaseCard.vue'

const router = useRouter()
const store = useNotificationStore()

const tab = ref<'inbox' | 'alerts'>('inbox')
const alerts = ref<Alert[]>([])
const stockMap = ref<Record<string, StockSummary>>({})

async function loadAlerts() {
  alerts.value = (await alertsApi.list()).data
}

onMounted(async () => {
  store.fetch().catch(() => {})
  loadAlerts().catch(() => {})
  try {
    const { data } = await stocksApi.list()
    stockMap.value = Object.fromEntries(data.map((s) => [s.code, s]))
  } catch {
    /* ignore */
  }
})

const sortedNotis = computed(() =>
  [...store.items].sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1)),
)

function nameOf(code: string) {
  return stockMap.value[code]?.name ?? code
}
function currencyOf(code: string) {
  return stockMap.value[code]?.currency ?? 'KRW'
}

function relTime(iso: string): string {
  const diff = (Date.now() - new Date(iso).getTime()) / 1000
  if (diff < 60) return '방금'
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`
  return `${Math.floor(diff / 86400)}일 전`
}

async function onNotiClick(id: number, read: boolean) {
  if (!read) await store.markRead(id).catch(() => {})
}

async function removeAlert(id: number) {
  await alertsApi.remove(id).catch(() => {})
  alerts.value = alerts.value.filter((a) => a.id !== id)
}
</script>

<template>
  <AppHeader title="알림" />
  <main class="noti">
    <div class="chips">
      <button :class="['chip', { 'chip--on': tab === 'inbox' }]" @click="tab = 'inbox'">
        받은 알림<span v-if="store.unreadCount" class="chip__badge">{{ store.unreadCount }}</span>
      </button>
      <button :class="['chip', { 'chip--on': tab === 'alerts' }]" @click="tab = 'alerts'">
        알림 설정
      </button>
    </div>

    <!-- 받은 알림 -->
    <template v-if="tab === 'inbox'">
      <div v-if="store.loaded && sortedNotis.length === 0" class="state">
        받은 알림이 없어요<br /><small>종목 상세에서 가격 알림을 설정해 보세요</small>
      </div>
      <ul v-else class="list">
        <li v-for="n in sortedNotis" :key="n.id">
          <BaseCard :class="['noti-card', { 'noti-card--unread': !n.read }]" @click="onNotiClick(n.id, n.read)">
            <div class="noti-card__top">
              <span class="noti-card__name">🔔 {{ nameOf(n.stockCode) }}</span>
              <span class="noti-card__time">{{ relTime(n.createdAt) }}</span>
            </div>
            <p class="noti-card__msg">{{ n.message }}</p>
          </BaseCard>
        </li>
      </ul>
    </template>

    <!-- 알림 설정 -->
    <template v-else>
      <div v-if="alerts.length === 0" class="state">
        설정한 알림이 없어요<br /><small>종목 상세에서 "가격 알림"을 추가하세요</small>
      </div>
      <ul v-else class="list">
        <li v-for="a in alerts" :key="a.id">
          <BaseCard class="alert-card">
            <div class="alert-card__body" @click="router.push(`/stocks/${a.stockCode}`)">
              <span class="alert-card__name">{{ nameOf(a.stockCode) }}</span>
              <span class="alert-card__cond tabular">
                {{ formatPrice(a.threshold, currencyOf(a.stockCode)) }}
                {{ ALERT_DIRECTION_LABEL[a.direction] }}
              </span>
            </div>
            <span :class="['alert-card__status', a.status === 'TRIGGERED' ? 'is-fired' : 'is-active']">
              {{ a.status === 'TRIGGERED' ? '발동됨' : '감시중' }}
            </span>
            <button class="alert-card__del" @click="removeAlert(a.id)">✕</button>
          </BaseCard>
        </li>
      </ul>
    </template>
  </main>
</template>

<style scoped>
.noti {
  flex: 1;
  padding: var(--space-5);
  padding-bottom: 84px;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.chips {
  display: flex;
  gap: var(--space-2);
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-sub);
}
.chip--on {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}
.chip__badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-pill);
  background: var(--color-danger);
  color: #fff;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.noti-card {
  cursor: pointer;
}
.noti-card--unread {
  border-left: 3px solid var(--color-primary);
}
.noti-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.noti-card__name {
  font-size: 14px;
  font-weight: 700;
}
.noti-card__time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.noti-card__msg {
  font-size: 14px;
  color: var(--color-text-sub);
  line-height: 1.4;
}
.alert-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.alert-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
}
.alert-card__name {
  font-size: 15px;
  font-weight: 700;
}
.alert-card__cond {
  font-size: 13px;
  color: var(--color-text-sub);
}
.alert-card__status {
  font-size: 11px;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: var(--radius-pill);
}
.is-active {
  background: var(--color-primary-weak);
  color: var(--color-primary);
}
.is-fired {
  background: color-mix(in srgb, var(--color-up) 15%, transparent);
  color: var(--color-up);
}
.alert-card__del {
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
  padding: var(--space-10) var(--space-2);
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 14px;
}
.state small {
  display: inline-block;
  margin-top: 6px;
  font-size: 12px;
}
</style>
