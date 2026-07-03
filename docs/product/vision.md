# StockPilot

> "당신의 투자를 조종하는 파일럿"

## 목표

사용자의 투자 성향(위험 성향 · 투자 기간)에 맞춰 개인화된 주식 종목을
추천하는 **AI 기반 실시간 개인 맞춤 주식 추천 플랫폼**을 구축한다.

## 핵심 가치

- 투자 성향 기반 개인화 추천으로 사용자 수익률 향상 지원
- 실시간 시세 이벤트를 안정적으로 수집·처리하는 이벤트 기반 아키텍처
- 캐시 · 메시지 브로커 · 동시성 제어 등 실무 백엔드 기술의 실증

## 아키텍처 개요

```
Open API (한국투자증권, Yahoo 등)
          │
          ▼
   Kafka Topic (Stock Price)
          │
   ┌──────┴──────┐
   ▼             ▼
Price Consumer   Analytics Consumer
   │             │
   ▼             ▼
Redis Cache    PostgreSQL
   │
   ▼
Spring Boot Recommendation API
   │
   ▼
 사용자
```

## 기술적 목표 (실무 역량 실증)

- **Kafka**: 실시간 시세 이벤트 수집·분산 처리 (Producer / 다중 Consumer)
- **Redis**: 추천 결과 캐싱, 인기 종목 랭킹(Sorted Set), 조회수/좋아요 원자 연산
- **PostgreSQL**: 사용자 정보 · 투자 성향 · 투자 이력 영속화
- **동시성 제어**: JPA 낙관적/비관적 락, Redis Atomic 연산, Kafka 순차 처리
- **인증/인가**: Spring Security + JWT
- **관측성**: Prometheus + Grafana + Micrometer
- **품질**: 단위 테스트, Testcontainers 기반 통합 테스트, k6/JMeter 부하 테스트

## 핵심 기능

| 기능 | 사용 기술 | 핵심 관심사 |
|------|-----------|-------------|
| 로그인 / 회원가입 | JWT + Spring Security | 인증/인가 |
| 관심종목 등록 | JPA + 낙관적 락 | 동시성 제어 |
| 추천 결과 조회 | Redis Cache (Cache-Aside) | 캐시 전략 |
| 실시간 시세 수집 | Kafka Producer | 비동기 이벤트 |
| 시세 저장 | Kafka Consumer | 이벤트 기반 처리 |
| 인기 종목 랭킹 | Redis Sorted Set | 고성능 랭킹 |
| 좋아요 | Redis Atomic → 배치 DB Sync | 원자성 · 정합성 |
| 가격 알림 | Kafka 이벤트 Consumer | 이벤트 드리븐 |

## 추천 알고리즘 (초안)

회원 가입 시 위험 성향(공격형/안정형/배당형)과 투자 기간(단기/장기)을 선택한다.
PER, PBR, ROE, 배당률, 최근 상승률, 거래량, 변동성 등을 점수화하여
성향별 가중치로 추천 점수를 계산한다.

```
추천점수 = 0.4 * f(PER) + 0.2 * f(ROE) + ...  (성향별 가중치 적용)
```

## 장기 목표

단순 CRUD를 넘어, 실무 수준의 캐시 · 메시지 브로커 · 동시성 제어 ·
이벤트 기반 아키텍처를 모두 갖춘 포트폴리오 수준의 서비스를 완성한다.
