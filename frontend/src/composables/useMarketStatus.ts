import { ref, onMounted, onUnmounted } from 'vue'

/** 주어진 타임존의 현재 시각이 평일 개장 시간(분 단위) 안인지 판단. */
function isOpen(timeZone: string, openMinutes: number, closeMinutes: number): boolean {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date())
  const weekday = parts.find((p) => p.type === 'weekday')?.value
  const hour = Number(parts.find((p) => p.type === 'hour')?.value)
  const minute = Number(parts.find((p) => p.type === 'minute')?.value)
  if (weekday === 'Sat' || weekday === 'Sun') return false
  const t = hour * 60 + minute
  return t >= openMinutes && t < closeMinutes
}

/**
 * 국내장(KRX 09:00~15:30 KST)·해외장(US 09:30~16:00 ET, DST 자동)의 개장 여부.
 * 1분마다 갱신. (공휴일은 반영하지 않음 — 시간대 기준 근사)
 */
export function useMarketStatus() {
  const krxOpen = ref(false)
  const usOpen = ref(false)

  function update() {
    krxOpen.value = isOpen('Asia/Seoul', 9 * 60, 15 * 60 + 30)
    usOpen.value = isOpen('America/New_York', 9 * 60 + 30, 16 * 60)
  }

  let timer: number | undefined
  onMounted(() => {
    update()
    timer = window.setInterval(update, 60_000)
  })
  onUnmounted(() => {
    if (timer) window.clearInterval(timer)
  })

  return { krxOpen, usOpen }
}
