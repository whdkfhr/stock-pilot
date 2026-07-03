# Roadmap

각 Phase는 하나 이상의 GitHub Issue로 분해되어 AI 파이프라인
(Planner → Architect → Implementer → Reviewer)을 통해 구현된다.

## Phase 0 — 프로젝트 부트스트랩 🚧

목표

- Spring Boot 프로젝트 스캐폴드
- Docker Compose 로컬 인프라(PostgreSQL, Redis, Kafka, Prometheus, Grafana)
- CI 파이프라인(build/test) 구성

## Phase 1 — 인증 / 회원

목표

- 회원가입 / 로그인 (JWT + Spring Security)
- 투자 성향(위험 성향 · 투자 기간) 등록
- 사용자 도메인 · 인증 필터

## Phase 2 — 종목 · 관심종목

목표

- 종목 마스터 데이터 관리
- 관심종목 등록/해제
- watch_count 동시성 제어 (낙관적 락)

## Phase 3 — 실시간 시세 수집 (Kafka)

목표

- 외부 Open API 시세 수집 스케줄러
- Kafka Producer (stock-price 토픽)
- Price Consumer → Redis 최신가 갱신
- Analytics Consumer → PostgreSQL 시세 저장

## Phase 4 — 추천 엔진

목표

- 성향 기반 추천 점수 계산
- 추천 결과 Redis 캐싱 (Cache-Aside)
- 추천 결과 조회 API

## Phase 5 — 랭킹 · 좋아요 · 조회수

목표

- 인기 종목 랭킹 (Redis Sorted Set, ZINCRBY)
- 좋아요 (Redis Atomic INCR → 배치 DB Sync)
- 조회수 집계

## Phase 6 — 알림

목표

- 사용자 가격 조건 등록
- 시세 이벤트 조건 매칭 Consumer
- 알림 생성/발송

## Phase 7 — 관측성 · 성능 검증

목표

- Micrometer 커스텀 메트릭
- Grafana 대시보드
- k6/JMeter 부하 테스트 및 TPS 비교 (직접 DB 저장 vs Kafka 경유)
