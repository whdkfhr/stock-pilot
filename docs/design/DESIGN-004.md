```markdown
# DESIGN-002

## Overview

로그인한 사용자가 종목을 관심종목으로 등록/해제하고, 종목의 관심 등록 수(watch_count)를 조회할 수 있도록 한다. 핵심 설계 과제는 동시 등록 상황에서 watch_count 집계의 정합성을 보장하는 것이다. 본 설계는 애플리케이션 레벨의 낙관적 락 대신, DB 원자적 증감 연산(atomic increment/decrement)을 통해 갱신 손실(lost update) 문제를 해결하는 방식을 채택한다.

종목 마스터 데이터의 생성/수정/삭제는 범위에서 제외하며, 관심종목 기능 동작에 필요한 최소한의 종목 조회만 다룬다.

## Architecture Overview

```
[Client]
   │ HTTP (JWT Bearer)
   ▼
[WatchlistController]
   │
   ▼
[WatchlistService] ──────────────┐
   │                             │
   ▼                             ▼
[WatchlistRepository]     [StockRepository]
   │                             │
   ▼                             ▼
[watchlist table]          [stock table]
```

- Controller: HTTP 요청/응답 매핑, 인증 컨텍스트에서 사용자 식별자 추출
- Service: 등록/해제 트랜잭션 처리, 중복 검증, watch_count 원자적 갱신 호출
- Repository: JPA 기반 CRUD + watch_count 갱신을 위한 네이티브/JPQL 벌크 업데이트 쿼리
- 동시성 제어: DB 레벨 원자적 UPDATE 문(`watch_count = watch_count + 1`)을 사용하여 row-level lock으로 정합성 보장. 애플리케이션 레벨 재시도 불필요.

## API Design

### [POST] /api/stocks/{stockId}/watch

인증된 사용자가 해당 종목을 관심종목으로 등록한다.

Request:
```
(No Body)
Header: Authorization: Bearer {token}
```

Response 201:
```
{
  "watchlistId": 1001,
  "stockId": 42,
  "userId": 7,
  "createdAt": "2024-06-01T10:15:30Z"
}
```

Error:
- 401: 인증되지 않은 요청 (토큰 없음/유효하지 않음)
- 404: 존재하지 않는 stockId
- 409: 이미 등록된 관심종목 (중복 등록)

---

### [DELETE] /api/stocks/{stockId}/watch

인증된 사용자가 해당 종목의 관심종목 등록을 해제한다.

Request:
```
(No Body)
Header: Authorization: Bearer {token}
```

Response 200:
```
{
  "stockId": 42,
  "unwatchedAt": "2024-06-01T11:00:00Z"
}
```

Error:
- 401: 인증되지 않은 요청
- 404: 관심종목으로 등록되어 있지 않음 (해제할 대상 없음)

---

### [GET] /api/me/watchlist

인증된 사용자의 관심종목 목록을 조회한다.

Request:
```
(No Body)
Header: Authorization: Bearer {token}
Query: page (optional, default 0), size (optional, default 20)
```

Response 200:
```
{
  "content": [
    {
      "watchlistId": 1001,
      "stockId": 42,
      "stockCode": "005930",
      "stockName": "삼성전자",
      "watchCount": 128,
      "createdAt": "2024-06-01T10:15:30Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

Error:
- 401: 인증되지 않은 요청

## Data Model

### Entity: Stock

관심종목 기능 동작을 위한 최소 조회 대상. 마스터 관리는 본 TASK 범위 외이며, 기존에 정의된 테이블을 참조한다는 전제 하에 필요한 필드만 명시한다.

| Field       | Type    | Description                          |
|-------------|---------|---------------------------------------|
| id          | Long    | PK                                    |
| code        | String  | 종목 코드                              |
| name        | String  | 종목명                                 |
| watch_count | Long    | 관심 등록 수 (동시성 제어 대상 필드), 기본값 0 |

### Entity: Watchlist

| Field       | Type      | Description                                   |
|-------------|-----------|------------------------------------------------|
| id          | Long      | PK                                             |
| user_id     | Long      | 관심종목을 등록한 사용자 ID (FK 개념, 논리적 참조) |
| stock_id    | Long      | 관심 등록된 종목 ID (FK 개념, 논리적 참조)         |
| created_at  | Timestamp | 등록 일시                                       |

제약 조건(논리):
- (user_id, stock_id) 조합은 유일해야 한다 (unique constraint).

## Package Structure

```
com.arok2.stockpilot
 ├── controller/
 │    └── WatchlistController
 ├── service/
 │    └── WatchlistService
 ├── domain/
 │    ├── Stock
 │    └── Watchlist
 ├── repository/
 │    ├── StockRepository
 │    └── WatchlistRepository
 └── dto/
      ├── WatchlistCreateResponse
      ├── WatchlistDeleteResponse
      └── WatchlistItemResponse
```

## Key Design Decisions

- **원자적 UPDATE 기반 watch_count 갱신**: `UPDATE stock SET watch_count = watch_count + 1 WHERE id = :stockId` 형태의 벌크 업데이트 쿼리를 사용한다. JPA 엔티티를 조회 후 필드를 변경하고 저장하는 방식(read-modify-write)은 동시 요청 시 갱신 손실이 발생하므로 배제한다. DB row-level lock이 자동으로 직렬화를 보장한다.
- **등록/해제와 watch_count 갱신의 원자적 트랜잭션 처리**: Watchlist row insert/delete와 Stock.watch_count 갱신은 하나의 트랜잭션(`@Transactional`) 내에서 처리되어, 부분 실패로 인한 데이터 불일치를 방지한다.
- **중복 등록 방지: DB Unique Constraint + 사전 검증 병행**: (user_id, stock_id) unique constraint를 DB 레벨에 두어 최종 정합성을 보장하고, Service 레벨에서 사전 존재 여부 확인 후 저장을 시도하여 일반적인 케이스에서 명확한 409 응답을 내려준다. Unique constraint 위반 시에도 예외를 409로 매핑한다(동시 중복 등록 요청에 대한 최종 방어선).
- **해제 시 존재하지 않는 관심종목에 대한 처리**: 해제 대상이 없으면 404를 반환하여 클라이언트가 상태를 명확히 인지하도록 한다.
- **watch_count는 Stock 엔티티에 비정규화하여 저장**: Watchlist 테이블에서 COUNT(*)로 실시간 집계하는 대신, Stock 테이블에 카운트를 비정규화하여 조회 성능을 확보한다. 등록/해제 시점에만 갱신 비용이 발생하고, 조회는 매우 빈번하므로 이 트레이드오프가 유리하다.

## Trade-offs

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| DB 원자적 UPDATE (`count = count + 1`) | 구현 단순, DB가 직렬화 보장, 재시도 로직 불필요 | 고빈도 갱신 시 특정 row에 대한 lock 대기 발생 가능 | ✅ |
| 애플리케이션 낙관적 락(@Version) + 재시도 | 락 경합 낮음 | 동시 등록 다발 시 재시도 로직 복잡, 실패 가능성 존재 | ❌ |
| Watchlist COUNT(*) 실시간 집계 (비정규화 없음) | 데이터 정합성 항상 보장, 별도 갱신 로직 불필요 | 목록 조회마다 COUNT 연산 비용 발생, 확장성 저하 | ❌ |
| 비정규화된 watch_count 컬럼 유지 | 조회 성능 우수 | 갱신 로직에 동시성 제어 필요 (원자적 UPDATE로 해결) | ✅ |

## Non-Functional Design

- **성능**: watch_count 조회는 Stock 테이블의 단일 컬럼 조회로 O(1) 수준이며, 목록 조회 시 N+1 문제를 피하기 위해 Watchlist-Stock join fetch 쿼리를 사용한다.
- **보안**: 모든 엔드포인트는 인증된 사용자만 접근 가능하며, 등록/해제/조회 시 요청 사용자의 식별자는 인증 컨텍스트(Security Context)에서만 추출한다. Path/Body를 통한 userId 조작은 허용하지 않는다.
- **확장성**: watch_count 갱신 쿼리는 stockId 단일 row에 대한 원자적 연산이므로, 종목 수가 증가해도 각 종목의 갱신은 독립적으로 확장 가능하다. 특정 인기 종목에 대한 갱신 경합이 심할 경우 추후 캐시/비동기 집계 방식으로 개선 가능하나 현재 범위에서는 불필요하다.

## Implementation Guide

1. **도메인/리포지토리 계층 우선 구현**
   - `Stock`, `Watchlist` 엔티티 정의 (기존 Stock 엔티티가 있다면 재사용, watch_count 필드 확인/추가)
   - `WatchlistRepository`: `findByUserIdAndStockId`, `existsByUserIdAndStockId`, `deleteByUserIdAndStockId` 메서드 정의
   - `StockRepository`: watch_count 원자적 증감을 위한 `@Modifying @Query` 메서드 두 개 정의
     - `incrementWatchCount(stockId)`: `UPDATE stock SET watch_count = watch_count + 1 WHERE id = :stockId`
     - `decrementWatchCount(stockId)`: `UPDATE stock SET watch_count = watch_count - 1 WHERE id = :stockId AND watch_count > 0`

2. **Service 계층 구현 (트랜잭션 경계 명확히)**
   - `register(userId, stockId)`:
     1. Stock 존재 여부 확인 (없으면 404 대응 예외)
     2. 중복 등록 여부 사전 확인 (있으면 409 대응 예외)
     3. Watchlist row 저장
     4. `incrementWatchCount` 호출
     5. 전체를 하나의 `@Transactional` 메서드로 묶는다
     6. DB unique constraint 위반 예외(DataIntegrityViolationException)를 catch하여 409로 변환하는 방어 로직 추가 (동시 중복 요청 대비)
   - `unwatch(userId, stockId)`:
     1. Watchlist row 존재 확인 (없으면 404 대응 예외)
     2. Watchlist row 삭제
     3. `decrementWatchCount` 호출
     4. 하나의 트랜잭션으로 처리
   - `getMyWatchlist(userId, pageable)`: Watchlist-Stock join하여 페이지 조회, DTO 변환

3. **Controller 계층 구현**
   - 인증 컨텍스트(예: `@AuthenticationPrincipal` 또는 동등한 메커니즘)에서 userId 추출 — Path/Body에서 절대 받지 않음
   - 예외를 각 HTTP 상태 코드(401/404/409)로 매핑하는 전역 예외 핸들러 활용 (기존 공통 예외 처리 체계가 있다면 그것을 따름)

4. **동시성 테스트 작성 시 주의사항**
   - 실제 DB(H2 또는 테스트 컨테이너 DB)를 사용하여 멀티 스레드로 동일 stockId에 대해 서로 다른 userId로 동시 등록 요청 실행
   - 각 스레드는 별도 트랜잭션으로 실행되어야 하므로, Service 메서드를 직접 멀티스레드에서 호출하거나 HTTP 레벨 통합 테스트로 검증
   - 테스트 종료 후 Stock.watch_count가 요청 수(N)와 정확히 일치하는지 검증

5. **주의사항**
   - watch_count 갱신은 반드시 벌크 업데이트 쿼리(`@Modifying`)로만 수행하고, 엔티티를 조회하여 setter로 변경 후 저장하는 방식은 사용하지 않는다 (갱신 손실 재발 방지).
   - 등록/해제 로직과 watch_count 갱신은 반드시 같은 트랜잭션 내에서 처리되어야 부분 실패를 방지할 수 있다.
   - 종목 마스터 데이터 관리(CRUD)는 구현하지 않는다. 이미 존재하는 Stock 데이터를 조회하는 용도로만 StockRepository를 사용한다.
```
