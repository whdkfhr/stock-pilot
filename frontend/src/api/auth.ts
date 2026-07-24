import { api } from './client'
import type {
  InvestmentPeriod,
  LoginRequest,
  LoginResponse,
  Me,
  RiskProfile,
  SignupRequest,
} from '@/types'

export const authApi = {
  signup(body: SignupRequest) {
    return api.post<{ id: number; email: string; nickname: string }>('/auth/signup', body)
  },
  login(body: LoginRequest) {
    return api.post<LoginResponse>('/auth/login', body)
  },
  me() {
    return api.get<Me>('/users/me')
  },
  updateProfile(body: { riskProfile: RiskProfile; investmentPeriod: InvestmentPeriod }) {
    return api.patch<Me>('/users/me', body)
  },
}
