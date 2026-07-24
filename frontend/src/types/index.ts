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

export type MarketType = 'KOSPI' | 'KOSDAQ' | 'NASDAQ' | 'NYSE'

export interface StockSummary {
  id: number
  code: string
  name: string
  market: MarketType
  currency: string
  price: number | null
  change: number | null
  changePercent: number | null
  watchCount: number
  likeCount: number
}

export interface StockDetail {
  id: number
  code: string
  name: string
  market: MarketType
  currency: string
  price: number | null
  change: number | null
  changePercent: number | null
  watchCount: number
  likeCount: number
  per: number
  pbr: number
  roe: number
  dividendYield: number
}

export interface ChartPoint {
  time: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export interface ChartResponse {
  code: string
  period: string
  points: ChartPoint[]
}

export interface QuoteResponse {
  code: string
  currency: string
  dayHigh: number | null
  dayLow: number | null
  volume: number | null
  week52High: number | null
  week52Low: number | null
  name: string | null
  exchange: string | null
}

export interface InvestorFlow {
  investor: string
  netBuy: number
}

export interface TradingTrend {
  code: string
  unit: string
  sample: boolean
  flows: InvestorFlow[]
}

export interface PriceHistoryPoint {
  code: string
  price: number
  volume: number
  tradedAt: string
}

export interface LikeResponse {
  code: string
  likeCount: number
}

export interface LikeStatus {
  code: string
  liked: boolean
  likeCount: number
}

export interface RecommendationItem {
  code: string
  name: string
  score: number
}

export interface Recommendation {
  userId: number
  riskProfile: RiskProfile
  generatedAt: string
  items: RecommendationItem[]
}

export interface RankingItem {
  rank: number
  code: string
  name: string
  viewCount: number
}

export interface WatchlistItem {
  watchlistId: number
  stockId: number
  stockCode: string
  stockName: string
  watchCount: number
  createdAt: string
}

export interface WatchlistPage {
  content: WatchlistItem[]
  page: number
  size: number
  totalElements: number
}

export type AlertDirection = 'ABOVE' | 'BELOW'
export type AlertStatus = 'ACTIVE' | 'TRIGGERED'

export interface Alert {
  id: number
  stockCode: string
  direction: AlertDirection
  threshold: number
  status: AlertStatus
  createdAt: string
  triggeredAt: string | null
}

export interface AlertCreateBody {
  stockCode: string
  direction: AlertDirection
  threshold: number
}

export interface NotificationItem {
  id: number
  stockCode: string
  message: string
  price: number
  read: boolean
  createdAt: string
}

export const ALERT_DIRECTION_LABEL: Record<AlertDirection, string> = {
  ABOVE: '이상',
  BELOW: '이하',
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
