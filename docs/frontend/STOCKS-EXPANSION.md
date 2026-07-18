# 종목 확장 · 검색 · 시장 구분 (국장/미장)

> 상태: ✅ 완료 (develop 병합) · P5 이전 선행 작업

## 배경
기존엔 종목이 5개뿐이고 수동 psql 삽입이라 재현 불가였다. 종목을 늘리고(국장+미장),
검색과 시장 필터를 붙인다.

## 백엔드
- **`MarketType`**(KOSPI/KOSDAQ/NASDAQ/NYSE) enum + 통화(KRW/USD) — `Stock.market` 필드 추가(nullable, null=KOSPI 호환).
- **`StockDataInitializer`**(CommandLineRunner, `stockpilot.seed.enabled`): 시작 시 ~20종목 upsert(코드 기준 멱등). KOSPI 12 · KOSDAQ 3 · NASDAQ 5.
- **YahooSymbolResolver**(시장 기반): KOSPI→`.KS`, KOSDAQ→`.KQ`, 미국→`{code}`. `YahooPriceSource`가 종목 시장을 조회(캐시)해 심볼 결정.
- **가격 표현**: 통화의 정수 단위(원/달러). 미국 종목은 센트 손실 허용(고가주라 오차 <0.5%).
- **DTO**: `StockSummaryResponse`/`StockDetailResponse`에 `market`·`currency` 추가.
- **검색**: `GET /api/stocks?q=` (이름/코드 부분일치, 대소문자 무시).
- 수집 간격 기본값 2s→5s(종목 수 증가에 따른 외부 호출 완화). 테스트는 시드/수집 비활성.

## 프론트
- `formatPrice(value, currency)` — USD `$`, KRW `원`. StockRow/상세/추천에 통화 반영.
- **홈**: 검색 입력 + 시장 필터 칩(전체/코스피/코스닥/미국) → 폴링 목록을 클라이언트 필터.

## 검증
- 백엔드 111개 테스트 green(검색·통화 포함).
- 라이브: 시드 ~20종목, 실 시세(국장 KRW/미장 USD), 검색·필터 동작 확인.

## 알려진 한계
- 미국 종목 가격은 정수 달러(센트 손실). 야후 비공식 소스 특성상 일부 국장 종목값 괴리 가능(→ KIS 승격 시 교정).
