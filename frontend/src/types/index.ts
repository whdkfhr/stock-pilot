// 백엔드 DTO와 1:1로 맞춘 타입.

export type RiskProfile = 'AGGRESSIVE' | 'STABLE' | 'DIVIDEND'
export type InvestmentPeriod = 'SHORT_TERM' | 'LONG_TERM'

export interface SignupRequest {
  email: string
  password: string
  nickname: string
  riskProfile: RiskProfile
  investmentPeriod: InvestmentPeriod
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface Me {
  id: number
  email: string
  nickname: string
  riskProfile: RiskProfile
  investmentPeriod: InvestmentPeriod
}

export interface ApiError {
  code: string
  message: string
  details?: { field: string; reason: string }[]
}

export const RISK_PROFILE_LABEL: Record<RiskProfile, string> = {
  AGGRESSIVE: '공격형',
  STABLE: '안정형',
  DIVIDEND: '배당형',
}

export const INVESTMENT_PERIOD_LABEL: Record<InvestmentPeriod, string> = {
  SHORT_TERM: '단기',
  LONG_TERM: '장기',
}
