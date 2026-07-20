# 상세 화면 대폭 보강 (캔들차트·시세요약·52주 게이지·종목정보)

> 상태: ✅ 완료 (develop 병합) · 5대 증권사 상세화면 참조

## 구현
### 백엔드
- **차트 OHLCV화**: `ChartPoint`를 `{time, open, high, low, close, volume}`로 확장.
  `YahooChartClient.parse`가 open/high/low/volume 배열까지 추출.
- **시세 요약**: `GET /api/stocks/{code}/quote` — Yahoo chart meta에서 고가/저가/거래량/
  52주 최고·최저/영문명/거래소. `QuoteService`/`QuoteController`/`QuoteMeta`/`QuoteResponse`.
  가격은 통화 정수 단위로 반올림. SecurityConfig permitAll. 테스트 118개 green.
- 시가총액은 Yahoo quoteSummary가 crumb 필요라 제외.

### 프론트
- **캔들 차트**(`CandleChart.vue`): 캔들(상승 빨강/하락 파랑) + 거래량 바 + 이동평균선(MA5/MA20).
  1일/1주/1달 기간 탭 유지(Sparkline → CandleChart 교체).
- **오늘의 시세** 카드: 고가/저가/거래량.
- **52주 최고·최저 게이지**: 현재가 위치 마커(토스 스타일).
- **종목 정보** 카드: 시장/거래소/영문명/배당수익률.

## 검증
- 백엔드 118 green(OHLCV·meta 파싱 포함).
- 라이브: quote(고저/거래량/52주), OHLCV 차트, 게이지 확인.

## 남은 후보 (다음)
- 캔들 지표(RSI/MACD), 시가총액·재무(별도 소스), 관련 뉴스, 호가창(KIS).
