# Frontend Phase 3 — 종목 상세

> 상태: ✅ 완료 (develop 병합)

## 목표
홈의 종목을 탭하면 상세로 이동해 **현재가·미니차트·투자지표·좋아요·관심종목**을 다룬다.

## 백엔드 추가
- **`GET /api/stocks/{code}`** (공개) — 종목 상세 + 투자지표.
  - `StockDetailResponse{ id, code, name, price, watchCount, likeCount, per, pbr, roe, dividendYield }`
  - 없는 코드 → `STOCK_NOT_FOUND`(404). `StockNotFoundException(String code)` 생성자 추가.
  - SecurityConfig: `GET /api/stocks/*` permitAll. 통합 테스트 추가(총 103개 green).

## 프론트 구현
- 라우트 `/stocks/:code`(requiresAuth). 홈의 모든 행 → 상세로 이동.
- **StockDetailView**: 진입 시 조회수 기록(POST /view) → 상세·이력·좋아요·관심 여부 병렬 로드.
  - 현재가 + 등락(이력 첫값 대비 절대/％, 국장 색)
  - **미니차트**: `Sparkline`(SVG) — 이력 20포인트를 시간순으로 area+line 렌더, 등락 색.
  - **투자지표** 그리드: PER/PBR/ROE/배당률.
  - **액션 바**: 좋아요(하트, 세션 내 1회 SADD 멱등), 관심종목 담기/해제(stockId로 watch/unwatch, 낙관적 갱신).

## 연동한 API
`GET /api/stocks/{code}`, `/price/history`, `/likes`, `POST /like`, `POST /view`,
`POST`·`DELETE /api/stocks/{stockId}/watch`, `GET /api/me/watchlist`

## 좋아요 토글 (후속 보강)
초기엔 좋아요가 등록만 되고 해제가 안 됐다(Phase 5의 동시성 시연용 SADD 멱등 설계). UI 기대에 맞춰 토글로 보강:
- 백엔드: `DELETE /api/stocks/{code}/like`(해제, SREM) + `GET /api/stocks/{code}/like/me`(내 좋아요 여부 SISMEMBER + 총 개수) 추가.
- 프론트: 진입 시 `like/me`로 상태 복원(새로고침해도 하트 유지) → 다시 클릭하면 해제.

## 실 시세 기본화
`stockpilot.price.source` 기본값을 **yahoo**로 변경(그냥 `bootRun`하면 실 국장 시세). 테스트는 `random`으로 고정(외부 의존 없음).

## 검증
- `npm run build` 통과, 백엔드 103개 green.
- 라이브(프록시): 상세(지표)·이력 20p·조회수 204·좋아요·관심 담기/해제·404 확인.

## 다음 (Phase 4 — 추천)
성향 기반 추천 리스트(점수 시각화), `GET /api/recommendations`.
