<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { extractErrorMessage } from '@/api/client'
import type { InvestmentPeriod, RiskProfile } from '@/types'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseSegmented from '@/components/ui/BaseSegmented.vue'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const password = ref('')
const nickname = ref('')
const riskProfile = ref<RiskProfile>('STABLE')
const investmentPeriod = ref<InvestmentPeriod>('LONG_TERM')
const error = ref('')
const loading = ref(false)

const canSubmit = computed(
  () => email.value && password.value.length >= 8 && nickname.value.length >= 2,
)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.signup({
      email: email.value.trim(),
      password: password.value,
      nickname: nickname.value.trim(),
      riskProfile: riskProfile.value,
      investmentPeriod: investmentPeriod.value,
    })
    router.replace('/')
  } catch (e) {
    error.value = extractErrorMessage(e, '회원가입에 실패했어요')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth">
    <div class="auth__intro">
      <h1 class="auth__title">투자의 시작,<br />StockPilot</h1>
      <p class="auth__subtitle">성향을 알려주면 맞춤 종목을 추천해 드려요</p>
    </div>

    <form class="auth__form" @submit.prevent="submit">
      <BaseInput
        v-model="email"
        type="email"
        label="이메일"
        placeholder="you@example.com"
        autocomplete="email"
      />
      <BaseInput
        v-model="password"
        type="password"
        label="비밀번호"
        placeholder="8자 이상"
        autocomplete="new-password"
      />
      <BaseInput v-model="nickname" label="닉네임" placeholder="2~20자" />

      <BaseSegmented
        v-model="riskProfile"
        label="투자 성향"
        :options="[
          { value: 'AGGRESSIVE', label: '공격형', hint: '수익 우선' },
          { value: 'STABLE', label: '안정형', hint: '가치 우선' },
          { value: 'DIVIDEND', label: '배당형', hint: '배당 우선' },
        ]"
      />
      <BaseSegmented
        v-model="investmentPeriod"
        label="투자 기간"
        :options="[
          { value: 'SHORT_TERM', label: '단기' },
          { value: 'LONG_TERM', label: '장기' },
        ]"
      />

      <p v-if="error" class="auth__error">{{ error }}</p>
      <BaseButton type="submit" :loading="loading" :disabled="!canSubmit">
        가입하고 시작하기
      </BaseButton>
    </form>

    <p class="auth__switch">
      이미 계정이 있나요?
      <RouterLink to="/login" class="auth__link">로그인</RouterLink>
    </p>
  </main>
</template>

<style scoped>
.auth {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: var(--space-8) var(--space-5) var(--space-6);
  gap: var(--space-6);
}
.auth__intro {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.auth__title {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.03em;
  color: var(--color-text-strong);
}
.auth__subtitle {
  font-size: 15px;
  color: var(--color-text-tertiary);
}
.auth__form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.auth__error {
  font-size: 14px;
  color: var(--color-danger);
  padding-left: 2px;
}
.auth__switch {
  text-align: center;
  font-size: 15px;
  color: var(--color-text-tertiary);
  margin-top: var(--space-2);
}
.auth__link {
  color: var(--color-primary);
  font-weight: 600;
}
</style>
