# Frontend Phase 6 — 마이 (UI 로드맵 완료)

> 상태: ✅ 완료 (develop 병합)

## 목표
👤 마이 탭: 프로필 조회 + **투자 성향 변경** + 관심종목 관리 + 로그아웃.

## 백엔드
- **`PATCH /api/users/me`**(인증): 투자 성향/기간 변경. `UpdateProfileRequest`,
  `UserService.updateProfile` → `User.updateProfile` + **추천 캐시 무효화**(`RecommendationCache.evict`).
  추천이 성향에 의존하므로 변경 즉시 재계산되도록 캐시를 비운다.
- 통합 테스트 추가(변경 반영 + evict 호출 검증). 총 121개 green.
- **시드 튜닝**: `기아` 지표가 전 항목 최상위라 모든 성향에서 1위였음 → 완화(per8/roe13/div4).
  시더가 기존 종목 지표도 갱신하도록 변경(시드=소스 오브 트루스). 결과: 성향별 1위 차별화
  (공격형→SK하이닉스, 안정형→현대차, 배당형→KT&G).

## 프론트
- **MyView**(/my, ComingSoon 교체): 프로필 카드(닉네임/이메일/성향·기간 배지 + "성향 변경"
  → BaseSegmented 편집 → PATCH), 관심종목 목록(현재가 + 삭제), 로그아웃.
- auth 스토어에 `updateProfile`. ComingSoonView 제거.

## 검증
- 라이브: 성향 STABLE→DIVIDEND PATCH → me 반영, 응답 riskProfile 재계산(캐시 무효화 확인),
  성향별 추천 1위 차별화 확인.

## UI 로드맵 완료 → 다음: KIS 승격 → SSE/WebSocket 실시간 전달
