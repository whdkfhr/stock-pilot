<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { INVESTMENT_PERIOD_LABEL, RISK_PROFILE_LABEL } from '@/types'
import AppHeader from '@/components/layout/AppHeader.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const auth = useAuthStore()
const router = useRouter()

const profileText = computed(() => {
  if (!auth.me) return ''
  return `${RISK_PROFILE_LABEL[auth.me.riskProfile]} · ${INVESTMENT_PERIOD_LABEL[auth.me.investmentPeriod]}`
})

function logout() {
  auth.logout()
  router.replace('/login')
}

// 다음 단계에서 채워질 기능들
const upcoming = [
  { icon: '🔥', title: '인기 랭킹', desc: '실시간 조회 TOP 종목' },
  { icon: '⭐', title: '관심종목', desc: '담아둔 종목 모아보기' },
  { icon: '🎯', title: '맞춤 추천', desc: '내 성향 기반 추천' },
  { icon: '🔔', title: '가격 알림', desc: '조건 도달 시 알림' },
]
</script>

<template>
  <AppHeader>
    <template #action>
      <button class="logout" @click="logout">로그아웃</button>
    </template>
  </AppHeader>

  <main class="home">
    <BaseCard class="hero">
      <p class="hero__hi">안녕하세요,</p>
      <h1 class="hero__name">{{ auth.me?.nickname ?? '투자자' }} 님 👋</h1>
      <div class="hero__profile">
        <span class="hero__badge">{{ profileText }}</span>
      </div>
    </BaseCard>

    <h2 class="section-title">곧 만나요</h2>
    <div class="grid">
      <BaseCard v-for="item in upcoming" :key="item.title" class="feature">
        <span class="feature__icon">{{ item.icon }}</span>
        <p class="feature__title">{{ item.title }}</p>
        <p class="feature__desc">{{ item.desc }}</p>
      </BaseCard>
    </div>

    <BaseButton variant="secondary" size="md" @click="auth.fetchMe()"> 내 정보 새로고침 </BaseButton>
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
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.hero__hi {
  font-size: 15px;
  color: var(--color-text-tertiary);
}
.hero__name {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
  margin-top: 2px;
}
.hero__profile {
  margin-top: var(--space-4);
}
.hero__badge {
  display: inline-block;
  padding: 6px 12px;
  background: var(--color-primary-weak);
  color: var(--color-primary);
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 600;
}
.section-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-strong);
  padding-left: 2px;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
}
.feature {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.feature__icon {
  font-size: 24px;
  margin-bottom: var(--space-2);
}
.feature__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-strong);
}
.feature__desc {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
</style>
