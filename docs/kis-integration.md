# KIS(한국투자증권) 실시간 시세 승격

> 상태: ✅ REST 하이브리드 완료 (develop) · 다음: WebSocket 체결가 스트리밍

## 개요
야후(15분 지연·폴링) → **KIS 국내주식 현재가(실시간·무지연)** 로 승격. `PriceSource` 교체(`stockpilot.price.source=kis`)만으로 붙어 나머지 파이프라인은 무변경.

- **하이브리드**: 국장(KOSPI/KOSDAQ) → KIS 실시간, 미장(NASDAQ) → 야후 유지(KIS 해외 실시간은 별도 신청).
- 실시간 확인: 삼성 현재가가 30초 내 변동, KIS 직접조회와 정확히 일치.

## 구성 요소
- `KisProperties`(stockpilot.kis): app-key/app-secret/base-url — **환경변수로만 주입, 커밋 금지**.
- `KisTokenClient`: OAuth `/oauth2/tokenP` 토큰 발급 + **24h 캐시**(KIS가 잦은 재발급 제한).
- `KisPriceSource`(source=kis): 국내 현재가 `/uapi/domestic-stock/v1/quotations/inquire-price`(tr_id FHKST01010100)
  파싱(stck_prpr/acml_vol/prdy_vrss+부호 → 전일종가). 미장은 `YahooPriceFetcher` 위임.
- `YahooPriceFetcher`: 야후 조회 로직을 소스 무관 재사용 컴포넌트로 분리(야후 소스 + KIS 하이브리드 공유).

## 초당 제한(EGW00201) 대응
KIS 실전은 다수 종목 지속 폴링 시 초당 제한을 확률적으로 반환한다.
- **쓰로틀**(호출 간 최소 600ms) + 실패 종목은 다음 주기에 갱신(자가치유). 재시도는 부하를 키워 역효과라 미사용.
- rate 스킵은 `KisRateLimitException`으로 구분해 **DEBUG로 조용히** 처리(진짜 실패만 WARN).
- 결과: WARN 실패 0, 전 종목 커버(15/15·5/5), 값은 실시간.
- **정석 해법**: 다수 종목 실시간은 REST 폴링이 아니라 **WebSocket 체결가 구독(H0STCNT0)** — 다음 단계.

## 보안
- 앱키/시크릿은 `${KIS_APP_KEY}` 등 플레이스홀더만 코드/설정에 존재. 실제 값은 실행 시 env로만.
- public 저장소이므로 키 파일/평문 커밋 금지(검증: 저장소 전체 키 문자열 검색 0건).
- 실전 키는 노출 시 KIS Developers에서 앱시크릿 재발급 권장.

## WebSocket 실시간 체결가 (승격 완료)
REST 폴링의 초당 제한을 없애고 진짜 틱 스트리밍을 위해 KIS WebSocket(H0STCNT0) 구독을 추가.
- `KisApprovalClient`(approval_key 발급) → `KisWebSocketClient`가 국내 종목을 구독해 체결 틱을
  Kafka(stock-price)로 발행 → 기존 파이프라인(캐시/SSE/알림) 재사용.
- WS 활성 시 REST 폴링(수집 스케줄러)은 국내를 생략(`wsHandlesDomestic`) → 중복/rate 완전 제거(EGW00201=0).
- **프레임 포맷**(실측): `0|H0STCNT0|<건수>|<데이터>`, 데이터는 `^` 구분·레코드당 46필드 반복.
  0=종목코드, 2=현재가, 4=전일대비(부호포함), 13=누적거래량. 전일종가=현재가-전일대비.
- 프래그먼트 누적, PINGPONG 자동 응답, 끊기면 재연결(approval_key 캐시 재사용). 실측 샘플로 파싱 단위테스트.
- 미장은 계속 야후(KIS 해외 실시간은 별도 신청). WS는 거래 시에만 push하므로 무거래 종목은
  다음 체결까지 이전 값을 유지한다.

## 실행
```bash
# REST(국내 실시간, 초당제한 자가치유)
KIS_APP_KEY=... KIS_APP_SECRET=... PRICE_SOURCE=kis ./gradlew bootRun

# WebSocket(국내 체결가 스트림, 권장) — WS 호스트(ops...:21000) 접속 가능한 환경에서
KIS_APP_KEY=... KIS_APP_SECRET=... PRICE_SOURCE=kis KIS_WEBSOCKET_ENABLED=true ./gradlew bootRun
# 모의: KIS_BASE_URL=https://openapivts...:29443 KIS_WS_URL=ws://ops.koreainvestment.com:31000
```
