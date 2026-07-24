# Frontend Phase 4 — 추천 + 하단 탭 내비게이션

> 상태: ✅ 완료 (develop 병합) · 백엔드 변경 없음(추천 API 기존)

## 목표
성향 기반 추천을 **점수 시각화**와 함께 보여주고, 앱 전역 **하단 탭 내비게이션**을 세운다.

## 구현
- **하단 탭(BottomNav)**: 홈 / 추천 / 알림 / 마이 (토스식 고정 하단 바).
  - `route.meta.tab`인 화면에서만 노출(App.vue). 종목 상세·로그인엔 표시 안 함.
  - 알림·마이는 아직 `ComingSoonView` 플레이스홀더(P5·P6에서 교체).
- **추천 화면(RecommendView)**: `GET /api/recommendations` + `GET /api/stocks`(가격 조인).
  - 성향 배지 + 설명, 종목별 **매칭 점수 바**(score 0..1 → %), 1위 강조.
  - 행 탭 → 종목 상세. 추천 새로고침 버튼.

## 연동한 API
`GET /api/recommendations`(인증), `GET /api/stocks`

## 검증
- `npm run build` 통과.
- 추천 API 성향별 차등 확인: 공격형→SK하이닉스(ROE), 배당형→KT&G(배당), score 0.26~0.83.
- 백엔드는 env 없이 기동해도 기본값 `yahoo`로 실 시세 확인(삼성 256,000 ≈ 야후 255,500).

## 작업 노트 (재발 방지)
커밋 전에 `git reset --hard`를 실행해 미커밋 프론트 수정이 롤백되는 사고가 있었음 →
**각 단계는 구현 직후 먼저 커밋하고, git 동기화/리셋은 그 뒤에** 수행한다.

## 다음 (Phase 5 — 알림)
알림 조건 등록/목록 + 알림 인박스(읽음). `/notifications` 탭 실제 구현.
