# CLAUDE.md

# StockPilot Project Constitution

Version: 1.0

> 이 문서는 프로젝트의 목적·기술 선택 근거·개발 규칙을 정의하는 최상위 기준 문서다.
> 세부 기획은 `docs/product/vision.md`, `docs/product/roadmap.md`,
> `docs/architecture/architecture.md`를 따른다. 충돌 시 이 문서가 우선한다.

---

# 1. 프로젝트를 시작한 이유 (Background)

이 프로젝트는 실제 개인적 계기에서 출발했다.

- 최근 **반도체 호황**을 계기로 주식 투자를 시작했다.
- 단순 매매를 넘어 **수익률을 높이기 위해**, 나의 **투자 성향에 맞는 종목을
  추천해 주는 서비스**가 있으면 좋겠다는 필요에서 기획했다.
- 동시에 이 프로젝트는 **백엔드 실무 역량을 실증하는 포트폴리오**를 목표로 한다.
  단순 CRUD가 아니라, 실무에서 쓰는 **캐시 · 메시지 브로커 · 동시성 제어 ·
  이벤트 기반 아키텍처**를 하나의 도메인 안에서 자연스럽게 다루는 것이 핵심이다.

즉 이 프로젝트의 성공 기준은 "기능 개수"가 아니라,
**기술 선택의 이유가 분명하고, 그 선택을 코드로 증명했는가**이다.

---

# 2. 무엇을 만드는가 (Product)

**StockPilot** — "당신의 투자를 조종하는 파일럿".
투자 성향(위험 성향 · 투자 기간)에 맞춰 개인화된 주식 종목을 추천하는
**AI 기반 실시간 개인 맞춤 주식 추천 플랫폼**.

핵심 흐름:

```
Open API(한국투자증권/Yahoo 등) → Kafka(실시간 시세) → Consumer
   → Redis(캐시/랭킹) + PostgreSQL(영속) → 추천 API → 사용자
```

---

# 3. 기획 파일 분석 — 기술 선택의 근거

`stock-pilot_.docx` 기획서가 정의한 "왜 이 기술을 쓰는가"를 요약한다.
아래는 각 기술이 이 프로젝트에서 **존재하는 이유**이며, 구현 시 반드시 이 의도를 지킨다.

## 3.1 Kafka — 왜 쓰는가
- 주식 시세는 초 단위로 끊임없이 유입된다. API가 시세를 **직접 DB에 넣으면**
  수천 TPS에서 DB가 병목이 된다.
- 그래서 `API → Kafka → Consumer → DB` 구조로 **수집과 저장을 분리**한다.
- 같은 시세 메시지를 여러 Consumer가 소비한다: 가격 저장 / 추천 계산 /
  알림 / 통계 / 로그. → **하나의 이벤트, 다수의 독립 소비자**가 Kafka를 쓰는 이유.

## 3.2 Redis — 왜 쓰는가
- **인기 종목 TOP-N**을 매번 `ORDER BY 조회수 DESC`로 조회하면 느리다.
  → Redis **Sorted Set(ZSET)** 으로 O(logN) 랭킹.
- **조회수 증가**는 클릭마다 DB를 건드리지 않고 Redis `ZINCRBY`로 처리.
- **추천 결과 캐싱**: 추천 계산이 2초 걸린다면 `user:{id}:recommend`에 캐싱해
  다음 조회는 수십 ms로 끝낸다 (Cache-Aside).

## 3.3 동시성 제어 — 이 프로젝트의 핵심 학습 포인트
기획서는 동시성 문제를 **의도적으로 만들고 해결**하도록 설계되어 있다.
- **관심종목 등록**: 100명이 동시에 `watch_count++` → lost update 발생.
- 해결책을 단계적으로 실증한다:
  1. JPA **낙관적 락**(`@Version`)
  2. JPA **비관적 락**
  3. Redis **Atomic**(INCR)
  4. Kafka **순차 처리**(파티션 키)
- **좋아요**도 동일: 1000명 동시 클릭 → Redis Atomic → 주기적 배치 DB Sync.
  → "왜 Kafka/Redis를 썼나?"에 명확히 답할 수 있는 시나리오를 코드로 남긴다.

## 3.4 추천 알고리즘
- 회원 가입 시 위험 성향(공격형/안정형/배당형)과 기간(단기/장기)을 선택.
- PER · PBR · ROE · 배당률 · 최근 상승률 · 거래량 · 변동성 등을 점수화하고
  **성향별 가중치**로 추천 점수를 계산한다.
  예: `추천점수 = 0.4·f(PER) + 0.2·f(ROE) + …`

## 3.5 그 외 기획 기능
- **실시간 랭킹**: 오늘 가장 많이 추천된 종목 TOP-20 (Redis Sorted Set).
- **알림**: 사용자가 "삼성전자 60,000원 이상" 설정 → 시세 이벤트가 Kafka로
  들어오면 Consumer가 조건 확인 → 알림 생성 (이벤트 드리븐).

## 3.6 기획서가 명시한 전체 기술 스택
Spring Boot · Spring Security + JWT · JPA · QueryDSL · PostgreSQL · Redis ·
Kafka · Docker · TestContainers · Prometheus · Grafana · Micrometer.

---

# 4. 아키텍처 원칙

- **레이어드 아키텍처**: Controller → Service → Repository.
- **도메인(기능) 단위 패키지**: base package `com.arok2.stockpilot`,
  하위 `auth / user / stock / watchlist / price / recommendation /
  ranking / notification / common`. (세부: `docs/architecture/architecture.md`)
- **이벤트 기반 처리**: 실시간 시세는 Kafka로 비동기 수집·분산 소비.
- **Cache-Aside**: 조회 성능이 중요한 데이터는 Redis 우선, DB 폴백.
- 단순함 우선. 불필요한 추상화·조기 최적화 금지.

---

# 5. 개발 표준

**기술**: Java 17 · Spring Boot 3.5 · Gradle.

**코딩 규칙**:
- 생성자 주입만 사용 (`@Autowired` 필드 주입 금지).
- Controller에 비즈니스 로직 금지. DTO로 API 계약 관리(엔티티 직접 노출 금지).
- Bean Validation, `@RestControllerAdvice` 글로벌 예외 처리, silent catch 금지.
- 비즈니스 로직은 단위 테스트 필수. 통합은 Testcontainers.

**테스트 실행**: `./gradlew test` — 테스트는 H2 프로파일을 사용하므로
로컬 인프라 없이도 통과해야 한다. 실제 Postgres/Kafka가 필요한 통합 테스트는
Testcontainers를 명시적으로 사용한다.

**로컬 인프라**: `docker compose up -d` (Postgres/Redis/Kafka/Prometheus/Grafana).

---

# 6. Git 워크플로우

- 메인 브랜치: `main`, `develop`.
- 기능 브랜치: `feature/task-{ID}` (AI 파이프라인이 자동 생성). PR base는 `develop`.
- **Conventional Commits**: `feat: / fix: / refactor: / docs: / test: / chore:`.
- 커밋은 작고 의미 있게.

---

# 7. AI 개발 파이프라인 (Enforcement)

이 저장소는 GitHub Issue 하나를 PR로 전환하는 **4-에이전트 파이프라인**으로
개발된다. 각 에이전트는 `.claude/agents/*.md` 프롬프트로 동작하며,
산출물을 dev-agent 오케스트레이션 서버(`SERVER_URL`)에 등록한다.
**기능 코드는 이 파이프라인이 생성하는 것을 원칙으로 한다** (사전 수작업 금지).

```
GitHub Issue
    ↓  Planner    → docs/tasks/TASK-{ID}.md      (Status: TODO)
    ↓  Architect  → docs/design/DESIGN-{ID}.md   (TASK → IN_PROGRESS)
    ↓  Implementer→ 소스 + 테스트, feature/task-{ID} PR (TASK → IN_REVIEW)
    ↓  Reviewer   → APPROVED → develop 자동 병합 / REJECTED → review_failed
```

## 7.1 INPUT LOCK — 각 에이전트는 선행 산출물이 없으면 중단한다
- **Planner**: GitHub Issue + `docs/product/vision.md` + `docs/product/roadmap.md`
- **Architect**: `docs/tasks/TASK-{ID}.md` (없으면 실행 불가)
- **Implementer**: `TASK-{ID}.md` + `docs/design/DESIGN-{ID}.md` (없으면 실행 불가)
- **Reviewer**: TASK + DESIGN + 소스 + 테스트

## 7.2 단일 책임 — 역할을 섞지 않는다
- Planner는 설계하지 않는다 (WHAT만, HOW 금지).
- Architect는 Java 코드를 쓰지 않는다 (구조·계약만).
- Implementer는 아키텍처/API 계약을 임의로 바꾸지 않는다.
- Reviewer는 코드를 고치지 않는다 (판정만).

## 7.3 금지 행위 (hard violations)
- TASK/DESIGN 없이 코드 작성.
- Reviewer APPROVED 전에 병합.
- 누락된 요구사항을 추측으로 채움.
- 비즈니스 로직 테스트 생략.

---

# 8. Claude 협업 원칙

Claude는 시니어 백엔드 엔지니어처럼 행동한다.
- 근거를 설명하고, 리스크를 짚고, 더 나은 설계를 제안하고,
  요구사항이 모호하면 질문한다.
- 요구사항을 지어내지 않고, 문서를 무시하지 않으며,
  아키텍처를 조용히 바꾸지 않고, 불필요한 복잡도를 만들지 않는다.

---

# 9. 문서 우선순위

충돌 시 위에서 아래 순으로 따르고, 해소되지 않으면 사용자에게 확인한다.

1. `CLAUDE.md` (이 문서)
2. `docs/product/` — vision.md, roadmap.md
3. `docs/decisions/adr/` — 아키텍처 결정 기록
4. `docs/architecture/architecture.md`
5. `docs/tasks/` — 현재 작업
6. 소스 코드

---

# 10. 이 문서에 대하여

살아있는 헌법이다. 새로운 원칙이 필요하면 개선하되,
핵심 원칙 변경은 그 이유를 ADR로 기록한다.
