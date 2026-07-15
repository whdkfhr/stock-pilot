<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { extractErrorMessage } from '@/api/client'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login({ email: email.value.trim(), password: password.value })
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (e) {
    error.value = extractErrorMessage(e, '이메일 또는 비밀번호를 확인해 주세요')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth">
    <div class="auth__intro">
      <span class="auth__logo">🛩️</span>
      <h1 class="auth__title">다시 오셨네요</h1>
      <p class="auth__subtitle">StockPilot에 로그인하고 맞춤 추천을 받아보세요</p>
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
        placeholder="비밀번호"
        autocomplete="current-password"
      />
      <p v-if="error" class="auth__error">{{ error }}</p>
      <BaseButton type="submit" :loading="loading" :disabled="!email || !password">
        로그인
      </BaseButton>
    </form>

    <p class="auth__switch">
      아직 계정이 없나요?
      <RouterLink to="/signup" class="auth__link">회원가입</RouterLink>
    </p>
  </main>
</template>

<style scoped>
.auth {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: var(--space-10) var(--space-5) var(--space-6);
  gap: var(--space-8);
}
.auth__intro {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.auth__logo {
  font-size: 40px;
  margin-bottom: var(--space-2);
}
.auth__title {
  font-size: 26px;
  font-weight: 700;
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
  margin-top: auto;
}
.auth__link {
  color: var(--color-primary);
  font-weight: 600;
}
</style>
