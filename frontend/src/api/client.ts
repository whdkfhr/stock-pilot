import axios, { AxiosError } from 'axios'
import type { ApiError } from '@/types'

const TOKEN_KEY = 'stockpilot.accessToken'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// 요청마다 JWT 부착
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 401이면 토큰 폐기 후 로그인으로 (인증 만료 처리는 라우터 가드가 담당)
api.interceptors.response.use(
  (res) => res,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      clearToken()
      if (location.pathname !== '/login') {
        location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

/** axios 에러에서 백엔드 ErrorResponse.message를 최대한 뽑아낸다. */
export function extractErrorMessage(err: unknown, fallback = '요청을 처리하지 못했어요'): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiError | undefined
    if (data?.details?.length) return data.details[0].reason
    if (data?.message) return data.message
    if (err.code === 'ERR_NETWORK') return '서버에 연결할 수 없어요 (백엔드가 실행 중인지 확인)'
  }
  return fallback
}
