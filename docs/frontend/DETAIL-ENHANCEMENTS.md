# 상세/홈 화면 보강 (차트·등락·수급·시장표시등)

> 상태: ✅ 완료 (develop 병합) · 사용자 화면 피드백 4건 반영

## 1. 시장 개장 표시등 (홈)
`useMarketStatus`: 국내장(KRX 09:00~15:30 KST)·해외장(US 09:30~16:00 ET, DST 자동)의
개장/휴장을 1분마다 판정(Intl 타임존). "실시간 시세 LIVE" 옆에 초록/회색 점으로 표시.

## 2. 기간별 차트 (상세)
기존 미니차트는 수집기가 쌓은 ~100초치라 밋밋했음 → **Yahoo 과거 캔들**을 직접 조회.
- 백엔드 `GET /api/stocks/{code}/chart?period=1D|1W|1M` — 시장별 심볼로 Yahoo chart 호출
  (1D=5분봉, 1W=30분봉, 1M=일봉). `YahooChartClient`/`ChartService`/`ChartController`.
  실패 시 빈 목록(화면은 동작). 공개 엔드포인트.
- 프론트: 상세에 1일/1주/1달 탭, Sparkline에 실제 캔들 종가를 렌더.

## 3. 투자자 매매동향 (상세, 샘플)
개인/외국인/기관 순매수는 KRX/KIS 전용 데이터라 Yahoo에 없음 → **"샘플" 라벨** 데모.
- 백엔드 `GET /api/stocks/{code}/trading-trend` — 국내 종목만 코드+날짜 시드로 안정적 합성값
  (순매수 합≈0). 미국 종목은 sample=false·빈 목록. KIS 연동 시 실데이터 교체.
- 프론트: 개인/외국인/기관 순매수(억원, 매수 빨강/매도 파랑) + 샘플 안내.

## 4. 등락가격(등락율) — 시세 목록/상세
전일 종가 대비 등락을 가격 아래에 표시.
- 백엔드: `StockPriceEvent`에 `previousClose` 추가(기존 4-인자 생성자 오버로드로 호환 유지),
  `LatestPriceCache`가 최신가+전일종가 저장, `StockSummary/DetailResponse`에 `change`·`changePercent`.
  Yahoo meta의 previousClose 사용.
- 프론트: `StockRow`가 가격 아래 등락가격(등락율)을 색(상승 빨강/하락 파랑)으로 표시.

## 검증
- 백엔드 117개 테스트 green(차트 파싱·매매동향·등락 계산 포함).
- 라이브: 기간 탭별 실캔들, 국장/미장 개장표시, 등락 표시, 수급 샘플 확인.
