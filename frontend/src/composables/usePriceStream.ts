import { onMounted, onUnmounted } from 'vue'

export interface PriceTick {
  code: string
  price: number
  change: number | null
  changePercent: number | null
}

/**
 * 실시간 시세 SSE(/api/stocks/stream)를 구독해 틱마다 handler를 호출한다.
 * 컴포넌트 언마운트 시 연결을 닫는다. (연결 끊김 시 EventSource가 자동 재연결)
 */
export function usePriceStream(onTick: (tick: PriceTick) => void) {
  let es: EventSource | null = null

  onMounted(() => {
    es = new EventSource('/api/stocks/stream')
    es.addEventListener('price', (e) => {
      try {
        onTick(JSON.parse((e as MessageEvent).data))
      } catch {
        /* ignore malformed */
      }
    })
  })

  onUnmounted(() => es?.close())
}
