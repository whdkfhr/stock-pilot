import { api } from './client'
import type { RankingItem, StockSummary, WatchlistPage } from '@/types'

export const stocksApi = {
  list() {
    return api.get<StockSummary[]>('/stocks')
  },
  popularRanking(limit = 10) {
    return api.get<RankingItem[]>('/rankings/popular', { params: { limit } })
  },
  myWatchlist(page = 0, size = 20) {
    return api.get<WatchlistPage>('/me/watchlist', { params: { page, size } })
  },
}
