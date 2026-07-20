/** 12345 → "12,345" */
export function formatNumber(n: number): string {
  return n.toLocaleString('ko-KR')
}

/** 원화 표기. null이면 대시. */
export function formatKRW(n: number | null | undefined): string {
  if (n == null) return '—'
  return `${formatNumber(n)}원`
}

/** 통화 기준 가격 표기. USD는 $, 그 외(KRW)는 원. null이면 대시. */
export function formatPrice(n: number | null | undefined, currency?: string): string {
  if (n == null) return '—'
  if (currency === 'USD') return `$${n.toLocaleString('en-US')}`
  return `${formatNumber(n)}원`
}

/** 부호 붙인 등락가격. 예: +1,200원, -$3 */
export function formatChange(change: number | null | undefined, currency?: string): string {
  if (change == null) return ''
  const sign = change > 0 ? '+' : change < 0 ? '-' : ''
  return sign + formatPrice(Math.abs(change), currency)
}

/** 부호 붙인 퍼센트. 예: +0.47% */
export function formatPercent(pct: number | null | undefined): string {
  if (pct == null) return ''
  const sign = pct > 0 ? '+' : ''
  return `${sign}${pct.toFixed(2)}%`
}

/** 등락 방향 → 색 토큰용 클래스 접미사 */
export type Direction = 'up' | 'down' | 'flat'

export function directionFromChange(change: number | null | undefined): Direction {
  if (change == null || change === 0) return 'flat'
  return change > 0 ? 'up' : 'down'
}

export function directionOf(prev: number | null, next: number | null): Direction {
  if (prev == null || next == null) return 'flat'
  if (next > prev) return 'up'
  if (next < prev) return 'down'
  return 'flat'
}
