# StockPilot Architecture

이 문서는 시스템 전반의 구조적 기준선(baseline)이다.
Architect 에이전트는 개별 DESIGN을 작성할 때 이 문서와의 일관성을 유지해야 한다.

## 1. 아키텍처 스타일

- **레이어드 아키텍처** (Controller → Service → Repository)
- **이벤트 기반 처리**: 실시간 시세는 Kafka를 통해 비동기 수집·분산 소비
- **Cache-Aside**: 조회 성능이 중요한 데이터는 Redis 우선 조회 후 DB 폴백

## 2. 기술 스택

| 영역 | 기술 |
|------|------|
| 언어 / 런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.5.x |
| 인증 | Spring Security + JWT |
| 영속성 | Spring Data JPA, QueryDSL, PostgreSQL |
| 캐시 | Redis (Lettuce) |
| 메시징 | Apache Kafka (Spring Kafka) |
| 관측성 | Actuator, Micrometer, Prometheus, Grafana |
| 테스트 | JUnit 5, Spring Boot Test, Testcontainers |
| 인프라 | Docker Compose |

## 3. 패키지 구조 (기준)

기능(도메인) 단위 패키지를 원칙으로 한다.

```
com.arok2.stockpilot
 ├── auth/              # 인증/인가 (JWT, Security)
 ├── user/             # 사용자, 투자 성향
 ├── stock/            # 종목 마스터
 ├── watchlist/        # 관심종목 (동시성)
 ├── price/            # 실시간 시세 (Kafka producer/consumer)
 ├── recommendation/   # 추천 엔진
 ├── ranking/          # 인기 랭킹 (Redis ZSET)
 ├── notification/     # 가격 알림
 └── common/           # 공통 (config, exception, response)
```

각 도메인 패키지 내부는 다음 하위 구조를 따른다:

```
{domain}/
 ├── controller/   # HTTP 레이어 (요청/응답만)
 ├── service/     # 비즈니스 로직
 ├── domain/      # 엔티티
 ├── repository/  # 퍼시스턴스
 └── dto/         # API 계약 (Record 권장)
```

## 4. 레이어 규칙

- Controller: HTTP 처리 및 Service 호출만. 비즈니스 로직 금지.
- Service: 비즈니스 로직, 트랜잭션 경계, Repository 호출.
- Repository: 퍼시스턴스 접근만.
- DTO: API 계약 전용. 도메인 엔티티를 API 응답으로 직접 노출하지 않는다.
- 의존성 주입: 생성자 주입만 사용 (`@Autowired` 필드 주입 금지).

## 5. 동시성 전략

| 대상 | 전략 |
|------|------|
| 관심종목 watch_count | JPA 낙관적 락(`@Version`) |
| 좋아요 / 조회수 | Redis Atomic(INCR) → 주기적 배치 DB Sync |
| 시세 순차 처리 | Kafka 파티션 키(종목코드) 기반 순서 보장 |

## 6. Kafka 토픽 (기준)

| 토픽 | Key | 설명 |
|------|-----|------|
| `stock-price` | 종목코드 | 실시간 시세 이벤트 |
| `price-alert` | userId | 알림 조건 매칭 이벤트 |

## 7. Redis 키 설계 (기준)

| 키 | 자료구조 | 용도 |
|----|----------|------|
| `stock:price:{code}` | String | 최신 시세 |
| `rank:popular` | Sorted Set | 인기 종목 랭킹 |
| `stock:like:{code}` | String(counter) | 좋아요 수 |
| `user:{id}:recommend` | String(JSON) | 추천 결과 캐시 |

## 8. 예외 처리

- `@RestControllerAdvice` 기반 글로벌 예외 핸들러.
- 표준 에러 응답 포맷을 `common`에 정의한다.
- silent catch 금지, 의미 있는 에러 메시지 필수.

## 9. 관측성

- `/actuator/prometheus`로 메트릭 노출.
- 도메인 핵심 지표는 Micrometer 커스텀 메트릭으로 계측.
