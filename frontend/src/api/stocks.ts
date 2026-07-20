import { api } from './client'
import type {
  ChartResponse,
  LikeResponse,
  LikeStatus,
  PriceHistoryPoint,
  RankingItem,
  StockDetail,
  StockSummary,
  TradingTrend,
  WatchlistPage,
} from '@/types'

export const stocksApi = {
  list() {
    return api.get<StockSummary[]>('/stocks')
  },
  detail(code: string) {
    return api.get<StockDetail>(`/stocks/${code}`)
  },
  history(code: string) {
    return api.get<PriceHistoryPoint[]>(`/stocks/${code}/price/history`)
  },
  chart(code: string, period: string) {
    return api.get<ChartResponse>(`/stocks/${code}/chart`, { params: { period } })
  },
  tradingTrend(code: string) {
    return api.get<TradingTrend>(`/stocks/${code}/trading-trend`)
  },
  popularRanking(limit = 10) {
    return api.get<RankingItem[]>('/rankings/popular', { params: { limit } })
  },
  myWatchlist(page = 0, size = 20) {
    return api.get<WatchlistPage>('/me/watchlist', { params: { page, size } })
  },
  recordView(code: string) {
    return api.post<void>(`/stocks/${code}/view`)
  },
  likes(code: string) {
    return api.get<LikeResponse>(`/stocks/${code}/likes`)
  },
  likeStatus(code: string) {
    return api.get<LikeStatus>(`/stocks/${code}/like/me`)
  },
  like(code: string) {
    return api.post<LikeResponse>(`/stocks/${code}/like`)
  },
  unlike(code: string) {
    return api.delete<LikeResponse>(`/stocks/${code}/like`)
  },
  watch(stockId: number) {
    return api.post(`/stocks/${stockId}/watch`)
  },
  unwatch(stockId: number) {
    return api.delete(`/stocks/${stockId}/watch`)
  },
}
