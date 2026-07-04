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

이 저장소는 dev-agent 오케스트레이션 서버와 연동되는 4-에이전트 파이프라인을 사용한다.
GitHub Issue 하나가 Planner → Architect → Implementer → Reviewer를 거쳐 자동으로 PR이 된다.

```
Issue + `plan` 라벨 ──▶ Planner ──(label: design)──▶ Architect ──(label: implement)──▶ Implementer ──▶ PR ──▶ Reviewer
                  │                              │                                │                      │
              docs/tasks/                   docs/design/                    feature/task-NNN 브랜치   APPROVE→auto-merge
                                                                                                       REJECT→review_failed
```

각 에이전트는 `.claude/agents/*.md`의 시스템 프롬프트로 Claude API를 호출하며,
산출물(Task/Design/Implementation/PR/Review)을 dev-agent 서버(`SERVER_URL`)에 등록한다.

### 최초 설정 (한 번만)

1. **Git 저장소 초기화 & GitHub 푸시**
   ```bash
   git init && git add . && git commit -m "chore: bootstrap StockPilot"
   gh repo create stock-pilot --private --source=. --push
   ```
2. **`develop` 브랜치 생성** — Implementer는 `develop`을 체크아웃하고 PR의 base로 사용한다.
   ```bash
   git checkout -b develop && git push -u origin develop
   ```
3. **GitHub Secrets 설정** (Settings → Secrets and variables → Actions)
   - `ANTHROPIC_API_KEY` — **필수**. Claude API 키.
   - `SERVER_URL` — *선택*. 미설정 시 `https://dev-agent-production-1459.up.railway.app`로 기본 동작.
4. **라벨 생성** — Actions 탭에서 **"Setup Labels"** 워크플로우를 `Run workflow`로 1회 실행.
5. **Auto-merge 활성화** — Settings → General → "Allow auto-merge" 체크
   (Reviewer가 APPROVED 시 `--auto` 병합을 사용).

### 사용

1. 로드맵의 한 항목을 GitHub Issue로 등록하고 **`plan` 라벨을 붙이면** Planner가 시작한다.
   (라벨은 triage 이상 권한자만 부착 가능 → 파이프라인 트리거 = 접근제어 겸 비용 게이트. 팀 전환 시 그대로 확장된다.)
2. `docs/tasks/TASK-NNN.md` → `docs/design/DESIGN-NNN.md` → `feature/task-NNN` PR 순으로 진행.
3. Reviewer가 APPROVED → `develop`에 자동 병합, REJECTED → 이슈에 `review_failed` 라벨.

> 참고: 워크플로우 4종(planner/architect/implementer/reviewer)과 setup-labels는
> dev-agent 프로젝트에서 이식했으며, `com.arok2.stockpilot` 패키지와 StockPilot
> 도메인에 맞게 조정되었다.
