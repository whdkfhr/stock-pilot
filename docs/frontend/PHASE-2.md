# Frontend Phase 2 — 홈

> 상태: ✅ 완료 (develop 병합)

## 목표
로그인 후 첫 화면(홈)에서 **실시간 시세 · 인기 랭킹 · 관심종목**을 한눈에 본다.

## 백엔드 추가 (프론트가 요구한 갭)
- **`GET /api/stocks`** (공개) — 전체 종목 목록 + 최신가.
  - `StockSummaryResponse{ id, code, name, price(null 가능), watchCount, likeCount }`
  - 관심종목 등록은 `stockId`(Long), 시세·좋아요·조회는 `code`를 쓰므로 **둘 다** 내려준다.
  - `StockQueryController`/`StockQueryService`, 최신가는 `LatestPriceCache`에서 조인.
  - SecurityConfig에 `GET /api/stocks` permitAll 추가. 통합 테스트 포함(총 101개 green).

## 프론트 구현
- **실시간 시세**: `useLiveStocks` 컴포저블이 `/api/stocks`를 4초 폴링, 직전값 대비 등락 방향(up/down)을 추적해 행 색을 갱신(국장 색: 상승 빨강, 하락 파랑) + LIVE 배지.
- **인기 랭킹**: `/api/rankings/popular?limit=5`, 순위·조회수 + 현재가(시세 맵에서 조인).
- **관심종목**: `/api/me/watchlist`, 비어있으면 안내(상세에서 추가 유도).
- 공통 컴포넌트: `StockRow`, `SectionHeader`, 그리고 P1의 `BaseCard`.

## 연동한 API
`GET /api/stocks`, `GET /api/rankings/popular`, `GET /api/me/watchlist`

## 검증
- `npm run build`(타입체크+빌드) 통과, 백엔드 101개 테스트 green.
- 라이브(프록시 경유): /api/stocks 5종목+현재가, 랭킹 TOP-5+가격 조인, 공개 200 확인.

## 다음 (Phase 3 — 종목 상세)
시세/미니차트(가격 이력) · 좋아요 · 관심종목 담기 · 조회수 기록.
홈의 각 행 → 상세로 이동(라우팅).
