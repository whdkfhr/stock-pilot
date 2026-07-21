# Frontend Phase 5 — 알림

> 상태: ✅ 완료 (develop 병합) · 백엔드 알림 API는 기존(Phase 6 backend)

## 목표
🔔 알림 탭을 실제 구현: 받은 알림 인박스 + 가격 알림 조건 관리 + 종목 상세에서 알림 설정.

## 구현
- **NotificationsView**(/notifications, ComingSoon 교체): 세그먼트 "받은 알림" | "알림 설정".
  - 받은 알림: 최신순 목록, 미읽음 강조(좌측 파란 바), 탭하면 읽음 처리.
  - 알림 설정: 내 조건 목록(종목명/임계가/방향 + 감시중·발동됨 배지 + 삭제), 탭 → 상세 이동.
- **종목 상세**: "🔔 가격 알림" 카드 — 방향(이상/이하) + 임계가(현재가 프리필) → `POST /api/alerts`.
- **알림 스토어**(Pinia): 미읽음 수 공유. **하단 탭 🔔에 미읽음 배지**. App에서 30초 폴링.

## 연동한 API
`POST/GET/DELETE /api/alerts`, `GET /api/notifications`, `PATCH /api/notifications/{id}/read`

## 검증
- `npm run build` 통과, 모듈 에러 0.
- 라이브 E2E: 조건 등록(ACTIVE) → 시세 이벤트 → 알림 생성 → 조건 TRIGGERED → 읽음 처리 확인.
  (백엔드 이벤트 드리븐: 시세 → AlertEvaluator → 알림)

## 다음
P6(마이) → KIS 승격 → SSE/WebSocket 실시간 전달.
