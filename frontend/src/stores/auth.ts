import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { clearToken, getToken, setToken } from '@/api/client'
import type { InvestmentPeriod, LoginRequest, Me, RiskProfile, SignupRequest } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const me = ref<Me | null>(null)
  const token = ref<string | null>(getToken())
  const loading = ref(false)

  const isAuthenticated = computed(() => !!token.value)

  async function login(body: LoginRequest) {
    const { data } = await authApi.login(body)
    setToken(data.accessToken)
    token.value = data.accessToken
    await fetchMe()
  }

  async function signup(body: SignupRequest) {
    await authApi.signup(body)
    // 가입 직후 자동 로그인
    await login({ email: body.email, password: body.password })
  }

  async function fetchMe() {
    if (!token.value) return
    loading.value = true
    try {
      const { data } = await authApi.me()
      me.value = data
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(body: { riskProfile: RiskProfile; investmentPeriod: InvestmentPeriod }) {
    const { data } = await authApi.updateProfile(body)
    me.value = data
  }

  function logout() {
    clearToken()
    token.value = null
    me.value = null
  }

  return { me, token, loading, isAuthenticated, login, signup, fetchMe, updateProfile, logout }
})
