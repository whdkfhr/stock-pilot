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
| Grafana | http://localhost:3000 (admin / admin) |

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
