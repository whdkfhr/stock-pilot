import { ref, onMounted, onUnmounted } from 'vue'

export type MarketSession = 'pre' | 'regular' | 'after' | 'closed'

export interface MarketState {
  session: MarketSession
  label: string
}

interface Range {
  from: number
  to: number
}

/** 타임존의 현재 시각(분)과 요일을 구한다. 주말이면 null. */
function nowMinutes(timeZone: string): number | null {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date())
  const weekday = parts.find((p) => p.type === 'weekday')?.value
  if (weekday === 'Sat' || weekday === 'Sun') return null
  const hour = Number(parts.find((p) => p.type === 'hour')?.value)
  const minute = Number(parts.find((p) => p.type === 'minute')?.value)
  return hour * 60 + minute
}

function inRange(t: number, r: Range): boolean {
  return t >= r.from && t < r.to
}

function classify(timeZone: string, pre: Range, regular: Range, after: Range): MarketSession {
  const t = nowMinutes(timeZone)
  if (t == null) return 'closed'
  if (inRange(t, regular)) return 'regular'
  if (inRange(t, pre)) return 'pre'
  if (inRange(t, after)) return 'after'
  return 'closed'
}

const h = (hour: number, min = 0) => hour * 60 + min

const KRX_LABEL: Record<MarketSession, string> = {
  pre: '장전',
  regular: '정규장',
  after: '시간외',
  closed: '휴장',
}
const US_LABEL: Record<MarketSession, string> = {
  pre: '프리마켓',
  regular: '정규장',
  after: '애프터마켓',
  closed: '휴장',
}

/**
 * 국내장(KRX)·해외장(US)의 세션 상태(장전/정규/시간외/휴장)를 1분마다 갱신.
 * - KRX(KST): 장전 08:30~09:00, 정규 09:00~15:30, 시간외 15:30~18:00
 * - US(ET, DST 자동): 프리 04:00~09:30, 정규 09:30~16:00, 애프터 16:00~20:00
 * (공휴일은 반영하지 않음 — 시간대 기준 근사)
 */
export function useMarketStatus() {
  const krx = ref<MarketState>({ session: 'closed', label: KRX_LABEL.closed })
  const us = ref<MarketState>({ session: 'closed', label: US_LABEL.closed })

  function update() {
    const k = classify(
      'Asia/Seoul',
      { from: h(8, 30), to: h(9) },
      { from: h(9), to: h(15, 30) },
      { from: h(15, 30), to: h(18) },
    )
    krx.value = { session: k, label: KRX_LABEL[k] }

    const u = classify(
      'America/New_York',
      { from: h(4), to: h(9, 30) },
      { from: h(9, 30), to: h(16) },
      { from: h(16), to: h(20) },
    )
    us.value = { session: u, label: US_LABEL[u] }
  }

  let timer: number | undefined
  onMounted(() => {
    update()
    timer = window.setInterval(update, 60_000)
  })
  onUnmounted(() => {
    if (timer) window.clearInterval(timer)
  })

  return { krx, us }
}
