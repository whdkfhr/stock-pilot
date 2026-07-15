import { api } from './client'
import type {
  LikeResponse,
  PriceHistoryPoint,
  RankingItem,
  StockDetail,
  StockSummary,
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
  like(code: string) {
    return api.post<LikeResponse>(`/stocks/${code}/like`)
  },
  watch(stockId: number) {
    return api.post(`/stocks/${stockId}/watch`)
  },
  unwatch(stockId: number) {
    return api.delete(`/stocks/${stockId}/watch`)
  },
}
