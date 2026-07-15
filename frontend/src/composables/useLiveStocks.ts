import { ref, onMounted, onUnmounted } from 'vue'
import { stocksApi } from '@/api/stocks'
import { directionOf, type Direction } from '@/utils/format'
import type { StockSummary } from '@/types'

/**
 * 전체 종목 시세를 주기적으로 폴링하며, 직전 값 대비 등락 방향을 추적한다.
 * (실시간 체감을 위한 클라이언트 폴링 — 백엔드 Kafka 파이프라인이 최신가를 갱신한다.)
 */
export function useLiveStocks(intervalMs = 4000) {
  const stocks = ref<StockSummary[]>([])
  const directions = ref<Record<string, Direction>>({})
  const loading = ref(true)
  const error = ref('')
  let timer: number | undefined

  async function refresh() {
    try {
      const { data } = await stocksApi.list()
      const prevByCode = new Map(stocks.value.map((s) => [s.code, s.price]))
      const nextDir: Record<string, Direction> = { ...directions.value }
      for (const s of data) {
        const dir = directionOf(prevByCode.get(s.code) ?? null, s.price)
        if (dir !== 'flat') nextDir[s.code] = dir
      }
      stocks.value = data
      directions.value = nextDir
      error.value = ''
    } catch {
      error.value = '시세를 불러오지 못했어요'
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    refresh()
    timer = window.setInterval(refresh, intervalMs)
  })
  onUnmounted(() => {
    if (timer) window.clearInterval(timer)
  })

  return { stocks, directions, loading, error, refresh }
}
