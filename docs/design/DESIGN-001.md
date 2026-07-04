기존 아키텍처 문서를 먼저 확인하겠습니다.

기존 `docs/architecture/architecture.md` 및 `docs/decisions/adr/` 문서가 제공되지 않았습니다. TASK-001은 프로젝트 초기 태스크(Phase 1 첫 단계)로 판단되며, 이 경우 회원가입 기능에 필요한 최소 범위의 아키텍처 전제(레이어 구조, 패키지 규칙)를 TASK 범위 내에서 설계에 포함하고, 이후 태스크들이 참조할 수 있는 형태로 작성합니다. 기존 아키텍처 문서가 실제로 존재한다면 충돌 여부를 확인해야 하므로, 이 점을 명시적으로 표기합니다.

---

```
# DESIGN-001

## Overview

이메일/비밀번호 기반 회원가입 기능을 설계한다. 사용자는 이메일, 비밀번호, 닉네임과 함께 투자 성향(위험 성향, 투자 기간)을 입력하여 계정을 생성한다. 본 설계는 TASK-001의 범위(가입 처리, 검증, 중복 방지, 비밀번호 안전 저장)만 다루며 로그인/인증/이메일 인증은 포함하지 않는다.

본 프로젝트에 기존 아키텍처 문서(`docs/architecture/architecture.md`)가 아직 확립되지 않은 것으로 판단되어, 본 설계에서 정의하는 레이어 구조와 패키지 규칙은 이후 태스크의 기준선이 된다. 기존 아키텍처 문서가 별도로 존재할 경우 충돌 여부 재검토가 필요하다.

## Architecture Overview

계층형 아키텍처(Layered Architecture)를 따른다.

```
[Client]
   │ HTTP
   ▼
Controller  ── 요청/응답 매핑, 입력 검증(형식)
   │
   ▼
Service     ── 비즈니스 규칙(중복 검사, 성향 값 검증, 등록 처리)
   │
   ▼
Repository  ── 영속성 접근
   │
   ▼
[Database]
```

- Controller는 DTO를 통해서만 외부와 통신한다.
- Service는 트랜잭션 경계를 가진다. 중복 이메일 검사와 저장은 하나의 트랜잭션 및 DB 유니크 제약으로 정합성을 보장한다.
- Domain(Entity)은 Controller에 직접 노출되지 않는다.

## API Design

### POST /api/auth/signup

Request:
```
{
  "email": "user@example.com",
  "password": "string (8~64자)",
  "nickname": "string (2~20자)",
  "riskProfile": "AGGRESSIVE | STABLE | DIVIDEND",
  "investmentPeriod": "SHORT_TERM | LONG_TERM"
}
```

Response 201:
```
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "string"
}
```

Error:
- 400: 요청 필드 누락, 형식 오류(이메일 형식 불일치, 비밀번호 길이 초과/미달), riskProfile/investmentPeriod 허용값 이외 입력
- 409: 이미 존재하는 이메일로 가입 시도 (중복)
- 500: 예기치 못한 서버 오류

Error Response Body:
```
{
  "code": "VALIDATION_ERROR | DUPLICATE_EMAIL | INTERNAL_ERROR",
  "message": "string",
  "details": [
    { "field": "email", "reason": "이메일 형식이 올바르지 않습니다" }
  ]
}
```
`details`는 VALIDATION_ERROR인 경우에만 포함되며, 그 외 에러 코드에서는 생략 가능하다.

## Data Model

### Entity: User

| Field             | Type      | Description                                  |
|-------------------|-----------|-----------------------------------------------|
| id                | Long      | PK                                            |
| email             | String    | 로그인 식별자, 유니크 제약                     |
| passwordHash      | String    | 단방향 해시(BCrypt 등)로 저장된 비밀번호       |
| nickname          | String    | 사용자 표시 이름                               |
| riskProfile       | Enum      | AGGRESSIVE, STABLE, DIVIDEND                  |
| investmentPeriod  | Enum      | SHORT_TERM, LONG_TERM                         |
| createdAt         | DateTime  | 생성 시각                                      |
| updatedAt         | DateTime  | 수정 시각                                      |

논리 제약:
- email: NOT NULL, UNIQUE
- passwordHash: NOT NULL
- nickname: NOT NULL
- riskProfile: NOT NULL
- investmentPeriod: NOT NULL

## Package Structure

```
com.arok2.stockpilot
 ├── controller/
 │    └── AuthController
 ├── service/
 │    └── AuthService
 ├── domain/
 │    ├── User
 │    ├── RiskProfile (enum)
 │    └── InvestmentPeriod (enum)
 ├── repository/
 │    └── UserRepository
 ├── dto/
 │    ├── request/
 │    │    └── SignupRequest
 │    ├── response/
 │    │    └── SignupResponse
 │    └── error/
 │         └── ErrorResponse
 └── exception/
      ├── DuplicateEmailException
      └── GlobalExceptionHandler
```

## Key Design Decisions

- **DB 유니크 제약을 정합성의 최종 방어선으로 사용**: 이메일 중복 검사를 애플리케이션 레벨(사전 조회)로만 처리하면 동시 요청 시 race condition으로 중복 저장이 가능하다. `email` 컬럼에 UNIQUE 제약을 두고, 저장 시 제약 위반 예외를 `DuplicateEmailException`으로 변환하여 409 응답으로 매핑한다. 이를 통해 동시성 상황에서도 단일 계정만 생성됨을 보장한다.

- **비밀번호는 단방향 해시로만 저장**: 원문 비밀번호 및 대칭키 암호화는 저장하지 않는다. 해시 알고리즘의 구체적 선택(BCrypt 등)은 구현 단계에서 결정하되, 반드시 복호화 불가능한 방식이어야 한다.

- **riskProfile, investmentPeriod는 Enum으로 모델링**: 허용된 값 집합 외 입력을 원천적으로 차단하고, 이후 추천 로직에서 타입 안전하게 사용할 수 있도록 한다.

- **Controller-Service-Repository 3계층 유지, 불필요한 추상화(Facade, UseCase 인터페이스 등) 도입하지 않음**: 현재 기능 복잡도에서 추가 레이어는 이해도만 낮춘다.

## Trade-offs

| Option | Pros | Cons | Selected |
|--------|------|------|----------|
| 애플리케이션 레벨 사전 중복 검사만 사용 | 구현 단순 | 동시 요청 시 중복 생성 가능 | ✕ |
| DB 유니크 제약 + 예외 변환 | 동시성 정합성 보장 | 예외 처리 로직 추가 필요 | ✓ |
| Enum 대신 String으로 riskProfile 저장 | 확장 유연 | 잘못된 값 저장 가능, 검증 로직 분산 | ✕ |
| Enum으로 riskProfile/investmentPeriod 모델링 | 타입 안정성, 검증 일원화 | 값 추가 시 코드 변경 필요 | ✓ |

## Non-Functional Design

- **성능**: 단일 사용자 등록 트랜잭션으로 처리, 별도 캐싱이나 비동기 처리 불필요. 이메일 컬럼 UNIQUE 인덱스로 중복 조회 성능 확보.
- **보안**: 비밀번호는 단방향 해시 저장, 응답 본문에는 절대 포함하지 않는다. 로그에도 비밀번호 원문이 남지 않도록 주의(구현 가이드 참조).
- **확장성**: riskProfile/investmentPeriod가 향후 추천 알고리즘의 입력으로 재사용될 것을 고려하여 Enum 기반 도메인 모델을 유지한다. 이메일 인증, 소셜 로그인 등은 이후 태스크에서 User 엔티티 확장으로 대응 가능하도록 필드 구조를 단순하게 유지한다.

## Implementation Guide

1. **Domain 정의**: `User` 엔티티와 `RiskProfile`, `InvestmentPeriod` Enum을 정의한다. `email`에 유니크 제약을 반드시 설정한다.
2. **Repository**: `UserRepository`에 `existsByEmail` 또는 저장 시 제약 위반을 활용하는 방식 중 하나를 선택하되, 최종 정합성은 반드시 DB 유니크 제약에 의존한다.
3. **DTO 및 검증**: `SignupRequest`에 이메일 형식, 비밀번호 길이(8~64자), 닉네임 길이(2~20자) 검증 규칙을 표준 검증 애노테이션 방식으로 적용한다. riskProfile/investmentPeriod는 Enum 역직렬화 실패 시 400으로 매핑되도록 예외 처리기를 구성한다.
4. **Service**: `AuthService.signup(request)` 흐름은 다음과 같다.
   - 입력값은 이미 Controller 단에서 형식 검증 완료된 상태로 전달받는다.
   - 비밀번호 해시 생성 후 `User` 생성.
   - Repository 저장 시 유니크 제약 위반 예외를 `DuplicateEmailException`으로 변환.
5. **예외 처리**: `GlobalExceptionHandler`에서 검증 실패(400), 중복 이메일(409), 그 외 예외(500)를 API Design에 정의된 `ErrorResponse` 포맷으로 일괄 변환한다.
6. **주의사항**:
   - 응답(`SignupResponse`)에는 비밀번호 필드를 절대 포함하지 않는다.
   - 요청/응답 로깅 시 비밀번호 필드는 마스킹 처리한다.
   - 동시성 테스트(통합 테스트)는 동일 이메일로 병렬 요청을 발생시켜 하나만 성공하고 나머지는 409를 받는지 검증한다.
7. **테스트 우선순위**: Service 단위 테스트(검증 실패, 중복, 정상 가입) → Repository 유니크 제약 테스트 → Controller 통합 테스트(E2E 시나리오) 순으로 작성을 권장한다.
```
