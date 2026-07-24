import { ref, onMounted, onUnmounted } from 'vue'
import { stocksApi } from '@/api/stocks'
import { usePriceStream } from '@/composables/usePriceStream'
import type { StockSummary } from '@/types'

/**
 * 전체 종목 시세를 초기 로드한 뒤, SSE(usePriceStream)로 실시간 틱을 받아 갱신한다.
 * 브라우저 폴링 대신 서버 push라 즉시 반영된다. (30초 안전망 폴링 유지)
 */
export function useLiveStocks() {
  const stocks = ref<StockSummary[]>([])
  const loading = ref(true)
  const error = ref('')
  let poll: number | undefined

  async function refresh() {
    try {
      const { data } = await stocksApi.list()
      stocks.value = data
      error.value = ''
    } catch {
      error.value = '시세를 불러오지 못했어요'
    } finally {
      loading.value = false
    }
  }

  usePriceStream((tick) => {
    const s = stocks.value.find((x) => x.code === tick.code)
    if (s) {
      s.price = tick.price
      s.change = tick.change
      s.changePercent = tick.changePercent
    }
  })

  onMounted(() => {
    refresh()
    poll = window.setInterval(refresh, 30_000) // 안전망(틱 유실 대비 재동기화)
  })
  onUnmounted(() => {
    if (poll) window.clearInterval(poll)
  })

  return { stocks, loading, error, refresh }
}
