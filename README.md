# StockPilot 🛩️

> "당신의 투자를 조종하는 파일럿" — AI 기반 개인 맞춤 주식 추천 플랫폼

투자 성향(위험 성향 · 투자 기간)에 맞춰 개인화된 종목을 추천하는 서비스.
실무 수준의 **캐시(Redis) · 메시지 브로커(Kafka) · 동시성 제어 · 이벤트 기반 아키텍처**를
Spring Boot로 구현하는 것을 목표로 한다.

기획 상세는 [`docs/product/vision.md`](docs/product/vision.md),
[`docs/product/roadmap.md`](docs/product/roadmap.md),
[`docs/architecture/architecture.md`](docs/architecture/architecture.md) 참고.

## 기술 스택

Java 17 · Spring Boot 3.5 · Spring Security + JWT · JPA + QueryDSL · PostgreSQL ·
Redis · Kafka · Actuator/Micrometer/Prometheus/Grafana · Testcontainers · Docker Compose

## 핵심 기능 & 릴리스

각 기능은 코드 + 테스트 + 실 인프라 라이브 데모로 검증하고 태그로 릴리스했다.

| 릴리스 | 기능 | 실증하는 것 |
|--------|------|-------------|
| v0.1.0 | 회원가입 | BCrypt, 유니크 제약 동시성 방어, 글로벌 예외 처리 |
| v0.2.0 | 로그인 / JWT | 무상태 인증, JWT 필터, 보호 자원 |
| v0.3.0 | 관심종목 등록/해제 | **동시성**: watch_count DB 원자적 UPDATE(갱신 손실 0) |
| v0.4.0 | Kafka 실시간 시세 수집 | 수집·저장 분리, 다중 Consumer 분산 소비 |
| v0.5.0 | 성향 기반 추천 | 가중치 스코어링 + **Cache-Aside**(Redis TTL) |
| v0.6.0 | 인기 랭킹 · 좋아요 | Redis **Sorted Set**(ZINCRBY/ZREVRANGE) · **Set**(SADD 멱등) |
| v0.7.0 | 이벤트 드리븐 알림 | 시세 이벤트 → 조건 평가, 원자적 발화(중복 방지) |
| v0.8.0 | 관측성 / 성능 | Micrometer 커스텀 메트릭 + Grafana 대시보드 |
| v0.9.0 | Yahoo 실 시세 연동 | `PriceSource` 교체만으로 목→실 데이터 전환 |

> 다음 단계: 한국투자증권(KIS) 실시간(WebSocket) 시세 승격.

## 아키텍처 개요

```
                                        ┌─▶ [price-cache]     ─▶ Redis (최신가)
 시세 소스        Collector    Kafka     │
 (PriceSource) ─▶ (스케줄러) ─▶ stock-price ┼─▶ [price-analytics] ─▶ PostgreSQL (시세 이력)
  random|yahoo|KIS              (파티션키=종목)│
                                        └─▶ [notification]    ─▶ 조건 평가 ─▶ 알림 생성
       (하나의 이벤트, 3개 독립 Consumer group)

 사용자 ─▶ REST API ─▶ Service ─┬─ Redis      (추천 캐시 / 랭킹 ZSET / 좋아요 Set)
   (JWT)                        └─ PostgreSQL (사용자·종목·관심종목·이력·알림 영속)
```

- **레이어드**: Controller → Service → Repository, 도메인 단위 패키지(`com.arok2.stockpilot.*`).
- **이벤트 기반**: 실시간 시세는 Kafka로 비동기 수집·분산 소비.
- **Cache-Aside**: 조회 성능이 중요한 데이터는 Redis 우선, DB 폴백.

## 기술 실증 — "왜 이 기술을 썼나"

| 주제 | 문제 | 해법 (코드) |
|------|------|-------------|
| **Kafka** | 초당 유입되는 시세를 API가 직접 DB에 넣으면 병목 | 수집→Kafka→Consumer로 분리, 한 토픽을 3개 group(캐시/이력/알림)이 독립 소비 |
| **Redis ZSET** | 인기 랭킹을 `ORDER BY count DESC`로 매번 조회하면 느림 | Sorted Set `ZINCRBY`/`ZREVRANGE`로 O(logN) 랭킹 |
| **Redis Set** | 다수 동시 좋아요 시 lost update·중복 | `SADD` 멱등(1인 1좋아요) + `SCARD` 정확 집계, 배치로 DB 동기화 |
| **동시성 제어** | 다수 동시 관심등록 시 watch_count 갱신 손실 | DB 원자적 `UPDATE ... SET watch_count = watch_count + 1` |
| **Cache-Aside** | 추천 계산 비용 | Redis 캐시 우선·미스 시 계산 후 캐싱(TTL), hit/miss 메트릭으로 적중률 관측 |
| **이벤트 드리븐** | 시세 조건 알림 | 시세 이벤트 소비 → 조건 평가 → 원자적 `ACTIVE→TRIGGERED`로 1회만 발화 |
| **관측성** | 아키텍처 효과를 수치로 증명 | Micrometer 커스텀 메트릭 → Prometheus → Grafana 대시보드 |

## 주요 API

인증이 필요한 엔드포인트는 `Authorization: Bearer <accessToken>` 헤더를 요구한다.

| 도메인 | 메서드 · 경로 | 인증 |
|--------|--------------|:---:|
| 인증 | `POST /api/auth/signup` · `POST /api/auth/login` | 공개 |
| 사용자 | `GET /api/users/me` | 🔒 |
| 관심종목 | `POST`·`DELETE /api/stocks/{id}/watch` · `GET /api/me/watchlist` | 🔒 |
| 시세 | `GET /api/stocks/{code}/price` · `GET /api/stocks/{code}/price/history` | 공개 |
| 추천 | `GET /api/recommendations` | 🔒 |
| 좋아요 | `POST /api/stocks/{code}/like` · `GET /api/stocks/{code}/likes` | 🔒 / 공개 |
| 랭킹 | `POST /api/stocks/{code}/view` · `GET /api/rankings/popular` | 공개 |
| 알림 조건 | `POST`·`GET /api/alerts` · `DELETE /api/alerts/{id}` | 🔒 |
| 알림 | `GET /api/notifications` · `PATCH /api/notifications/{id}/read` | 🔒 |
| 메트릭 | `GET /actuator/prometheus` | 공개 |

## 실시간 시세 소스

시세 공급은 `PriceSource` 인터페이스로 추상화되어 있어 **구현체 교체만으로** 목↔실 데이터를 전환한다.
`stockpilot.price.source`(환경변수 `PRICE_SOURCE`)로 선택한다.

| 값 | 소스 | 비고 |
|----|------|------|
| `kis` | 한국투자증권(하이브리드) | **국장 실시간 무지연**(KIS REST), 미장은 야후. 앱키/OAuth 필요 |
| `yahoo` (기본) | Yahoo Finance | 국장 `.KS`/`.KQ`, 약 15분 지연, 키 불필요 |
| `random` | 랜덤워크 목 | 외부 의존 없음(오프라인/CI). 테스트는 항상 이 값 |

```bash
./gradlew bootRun                    # 기본: 야후 실 시세(.KS 자동 매핑)
PRICE_SOURCE=random ./gradlew bootRun # 외부 의존 없이 목 시세로

# KIS 실시간(국장). 앱키는 절대 커밋 금지 — 환경변수로만 주입.
KIS_APP_KEY=... KIS_APP_SECRET=... PRICE_SOURCE=kis ./gradlew bootRun
# 모의투자: KIS_BASE_URL=https://openapivts.koreainvestment.com:29443
```

> KIS는 OAuth 토큰(24h 캐시)으로 국내주식 현재가를 조회한다. 다수 종목 REST 폴링은 초당 제한이
> 있어 일부 틱을 건너뛸 수 있으나(자가치유), 값은 지연 없이 실시간이다. 진짜 틱 단위 스트리밍은
> KIS WebSocket(체결가 구독)으로 승격 예정.

## 로컬 실행

```bash
# 1. 인프라 기동 (Postgres, Redis, Kafka, Kafka-UI, Prometheus, Grafana)
docker compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun

# 3. 테스트 (인프라 없이도 통과 — 테스트는 H2 프로파일 사용)
./gradlew test
```

| 서비스 | 주소 |
|--------|------|
| App | http://localhost:8080 |
| Actuator / Prometheus 메트릭 | http://localhost:8080/actuator/prometheus |
| Kafka UI | http://localhost:8081 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / admin) — "StockPilot 관측성" 대시보드 자동 프로비저닝 |

## AI 개발 파이프라인 (GitHub Actions)

이 저장소는 GitHub Issue 하나를 자동으로 PR로 전환하는 **자가치유(self-healing) 5-에이전트
파이프라인**을 사용한다. 모든 단계는 **이슈 라벨**로 트리거되며, Reviewer가 반려하면 Fix
에이전트가 스스로 패치를 시도(최대 3회)한 뒤 초과 시 사람에게 넘긴다.

```
Issue +plan ─▶ Planner ─(design)▶ Architect ─(implement)▶ Implementer ─▶ PR + 이슈 review 라벨
                                                                                     │
   ┌─────────────────────────────────────────────────────────────────────────────  ▼
   │  Reviewer  ①빌드·테스트 게이트(./gradlew test)  →  ②통과 시에만 Claude 리뷰
   │      ├─ APPROVED  → develop 자동 병합 + done
   │      └─ REJECTED  → review_failed
   │                          │
   │                          ▼
   │              Fix 에이전트(자가치유): 리뷰/빌드 로그로 패치 → retry_N → 다시 review 라벨
   └──────────────  3회 초과 시 → fix_failed (사람이 수동 마무리)
```

- 트리거는 전부 **이슈 라벨**(`plan`→`design`→`implement`→`review`→`review_failed`).
  라벨을 붙일 수 있는 사람(triage 이상 권한)이 곧 접근제어다.
- 각 에이전트는 `.claude/agents/*.md` 프롬프트로 Claude API(Sonnet 5)를 호출하고,
  산출물(Task/Design/Implementation/PR/Review)을 dev-agent 서버(`SERVER_URL`)에 등록한다.

### 신뢰성 업그레이드

파이프라인 에이전트는 **기존 코드를 직접 보지 못한다**(TASK/DESIGN/diff 텍스트만 봄). 이로
인한 컴파일 오류·회귀를 줄이기 위해 두 가지를 적용했다:

1. **빌드·테스트 게이트 (reviewer)** — Claude 리뷰 전에 CI에서 실제 `./gradlew test`를 돌린다.
   실패하면 Claude 호출 없이 **자동 반려**하고 빌드 로그를 Fix 에이전트에 넘겨 컴파일 오류·
   회귀를 사람 손 전에 걸러낸다.
2. **저장소 인식 컨텍스트 (implementer)** — 기존 소스 파일 목록 + 핵심 연동 파일
   (SecurityConfig, GlobalExceptionHandler, JWT/인증 등)의 전체 내용을 프롬프트에 주입해
   기존 코드를 보존하며 확장하도록 유도한다.

### 최초 설정 (한 번만)

1. **기본 브랜치를 `develop`으로** — 파이프라인은 develop에서 동작하고 PR도 develop을 base로 한다. `main`은 릴리스 브랜치.
2. **GitHub Secrets** (Settings → Secrets and variables → Actions)
   - `ANTHROPIC_API_KEY` — **필수**. Claude API 키.
   - `PIPELINE_TOKEN` — **필수**. fine-grained PAT(대상 저장소, Contents·Issues·Pull requests: Read/Write).
     기본 `GITHUB_TOKEN`은 라벨/PR로 다음 워크플로우를 트리거하지 못하므로(재귀 방지)
     핸드오프에 이 PAT를 사용한다.
   - `SERVER_URL` — *선택*. 미설정 시 `https://dev-agent-production-1459.up.railway.app`로 기본 동작.
3. **라벨 생성** — Actions 탭에서 **"Setup Labels"** 워크플로우를 `Run workflow`로 1회 실행.
4. **Auto-merge 활성화** — Settings → General → "Allow auto-merge" 체크.

### 사용

1. 로드맵의 한 항목을 GitHub Issue로 등록하고 **`plan` 라벨을 붙이면** 파이프라인이 시작된다.
2. `TASK-NNN` → `DESIGN-NNN` → `feature/task-NNN` PR → 리뷰 순으로 자동 진행된다.
3. Reviewer가 APPROVED → `develop`에 자동 병합(+`done`), REJECTED → Fix 자가치유(최대 3회).
4. `fix_failed`가 되면(리뷰 기준 미충족·기존 코드 얽힌 회귀 등) 사람이 이어받아 마무리한다.

> 참고: 워크플로우 6종(planner/architect/implementer/reviewer/fix/setup-labels)은
> dev-agent 프로젝트에서 이식·확장했으며, `com.arok2.stockpilot` 패키지와 StockPilot
> 도메인에 맞게 조정되었다. 기존 코드와 얽히는 기능은 파이프라인이 회귀를 낼 수 있어
> 수동 마무리가 필요할 수 있다(자세한 배경은 커밋 히스토리 참고).
