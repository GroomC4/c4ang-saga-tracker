# Saga Tracker Service - 프로젝트 개요

## 1. 프로젝트 목적

Saga Tracker Service는 C4ang MSA 쇼핑몰 시스템에서 발생하는 모든 Saga(분산 트랜잭션)의 진행 상황을 중앙에서 추적하고 모니터링하는 서비스입니다.

### 1.1 핵심 목표
- **중앙 집중식 추적**: Order, Product, Payment 등 분리된 MSA의 Saga 진행 상황을 한 곳에서 관리
- **감사 추적성**: 최소 30일 이상 전체 Saga 타임라인 복원 가능
- **관측 가능성**: Grafana/Prometheus 대시보드와 연동된 실시간 모니터링
- **장애 복구**: DLQ 기반 재처리 및 멱등성 보장

### 1.2 해결하는 문제
1. 분산 환경에서 Saga 상태를 서비스별로 조회하기 어려움
2. Cross-domain Saga 조회 시 조인 비용 및 복잡도
3. 공통 규칙 복제로 인한 Drift 위험
4. 감사/모니터링/알림의 분산화로 인한 운영 어려움

## 2. 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Domain Services                              │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐        │
│  │  Order    │  │  Product  │  │  Payment  │  │   Store   │        │
│  │  Service  │  │  Service  │  │  Service  │  │  Service  │        │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘        │
│        │              │              │              │               │
│        └──────────────┴──────────────┴──────────────┘               │
│                              │                                       │
│                    SagaTrackerClient SDK                            │
│                              │                                       │
└──────────────────────────────┼───────────────────────────────────────┘
                               ▼
                    ┌─────────────────────┐
                    │  Kafka              │
                    │  c4ang.saga.tracker │
                    │  (3 partitions)     │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────────────────────┐
                    │       Saga Tracker Service          │
                    │  ┌────────────────────────────────┐ │
                    │  │     Kafka Consumer             │ │
                    │  │   (saga-tracker-service)       │ │
                    │  └────────────────────────────────┘ │
                    │                │                    │
                    │  ┌─────────────┴───────────────┐   │
                    │  │    Application Service      │   │
                    │  └─────────────┬───────────────┘   │
                    │                │                    │
                    │  ┌─────────────┴───────────────┐   │
                    │  │      PostgreSQL             │   │
                    │  │  saga_instance / saga_steps │   │
                    │  └─────────────────────────────┘   │
                    │                                     │
                    │  ┌─────────────────────────────┐   │
                    │  │    REST API                 │   │
                    │  │  GET /sagas/{sagaId}        │   │
                    │  │  GET /sagas?orderId=...     │   │
                    │  └─────────────────────────────┘   │
                    │                                     │
                    │  ┌─────────────────────────────┐   │
                    │  │  Prometheus Exporter        │   │
                    │  │  /actuator/prometheus       │   │
                    │  └─────────────────────────────┘   │
                    └─────────────────────────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                   ▼
           ┌───────────────┐                   ┌───────────────┐
           │   Grafana     │                   │  Alert        │
           │  Dashboard    │                   │  (Slack/PD)   │
           └───────────────┘                   └───────────────┘
```

## 3. 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Kotlin | 2.0.21 |
| JDK | OpenJDK | 21 |
| Framework | Spring Boot | 3.3.4 |
| Build | Gradle Kotlin DSL | 8.x |
| Database | PostgreSQL | 15.x |
| Messaging | Apache Kafka | 3.x |
| Schema | Avro + Schema Registry | 7.5.1 |
| Metrics | Micrometer + Prometheus | - |
| Testing | Kotest, MockK, Testcontainers | - |

## 4. 주요 기능

### 4.1 Kafka Consumer
- `c4ang.saga.tracker` 토픽 구독 (Consumer Group: `saga-tracker-service`)
- Avro 메시지 역직렬화 (`SagaTracker.avsc` 기반)
- 멱등 처리 (`eventId` 기반 중복 방지)
- DLQ 처리 (`c4ang.saga.tracker.dlt`)

### 4.2 데이터 저장
- `saga_instance`: Saga 인스턴스 메인 테이블
- `saga_steps`: Saga 단계별 이력 테이블
- JSONB를 활용한 metadata 저장

### 4.3 REST API
- `GET /sagas/{sagaId}`: 특정 Saga 조회
- `GET /sagas?orderId=&status=&type=`: Saga 검색
- `GET /sagas/{sagaId}/steps`: Saga 단계 조회

### 4.4 모니터링
- Prometheus 메트릭 노출
- Grafana 대시보드 연동
- AlertRule 기반 알림 (Slack/PagerDuty)

## 5. 데이터 보존 정책

| 저장소 | 보존 기간 | 비고 |
|--------|----------|------|
| Kafka `saga.tracker` | 30일 | retention.ms=2592000000 |
| PostgreSQL | 90일 | Active storage |
| Cold Storage (S3) | 1년+ | 아카이브 (추후) |

## 6. 관련 문서

- [02-ARCHITECTURE.md](./02-ARCHITECTURE.md) - 상세 아키텍처
- [03-DATABASE-SCHEMA.md](./03-DATABASE-SCHEMA.md) - 데이터베이스 스키마
- [04-API-SPECIFICATION.md](./04-API-SPECIFICATION.md) - API 명세
- [05-MONITORING.md](./05-MONITORING.md) - 모니터링 설계
- [06-DOMAIN-SERVICE-INTEGRATION.md](./06-DOMAIN-SERVICE-INTEGRATION.md) - 도메인 서비스 연동 가이드
- [TODO.md](./TODO.md) - 작업 목록
