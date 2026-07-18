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

/** 등락 방향 → 색 토큰용 클래스 접미사 */
export type Direction = 'up' | 'down' | 'flat'

export function directionOf(prev: number | null, next: number | null): Direction {
  if (prev == null || next == null) return 'flat'
  if (next > prev) return 'up'
  if (next < prev) return 'down'
  return 'flat'
}
