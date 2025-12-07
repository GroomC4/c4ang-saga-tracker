# Saga Tracker Service - 아키텍처 상세 설계

## 1. Hexagonal Architecture

Saga Tracker Service는 다른 C4ang 서비스들과 동일하게 Hexagonal Architecture(Ports & Adapters)를 적용합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Application                                 │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                     Domain Layer                               │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐   │ │
│  │  │ SagaInstance│  │  SagaStep   │  │ SagaTrackerEvent    │   │ │
│  │  │   (Entity)  │  │   (Entity)  │  │   (Domain Event)    │   │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘   │ │
│  │                                                                │ │
│  │  ┌─────────────────────────────────────────────────────────┐  │ │
│  │  │                   Domain Ports                           │  │ │
│  │  │  LoadSagaPort | SaveSagaPort | SagaMetricsPort         │  │ │
│  │  └─────────────────────────────────────────────────────────┘  │ │
│  └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                   Application Layer                            │ │
│  │  ┌─────────────────────┐  ┌─────────────────────────────────┐│ │
│  │  │  SagaEventProcessor │  │      SagaQueryService           ││ │
│  │  │  (Kafka Consumer)   │  │   (API Query Handler)           ││ │
│  │  └─────────────────────┘  └─────────────────────────────────┘│ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┴─────────────────────┐
        ▼                                           ▼
┌───────────────────────┐               ┌───────────────────────┐
│   Inbound Adapters    │               │   Outbound Adapters   │
│ ┌───────────────────┐ │               │ ┌───────────────────┐ │
│ │ Kafka Listener    │ │               │ │ JPA Repository    │ │
│ │ (SagaTracker msg) │ │               │ │ (PostgreSQL)      │ │
│ └───────────────────┘ │               │ └───────────────────┘ │
│ ┌───────────────────┐ │               │ ┌───────────────────┐ │
│ │ REST Controller   │ │               │ │ Metrics Adapter   │ │
│ │ (Query API)       │ │               │ │ (Micrometer)      │ │
│ └───────────────────┘ │               │ └───────────────────┘ │
└───────────────────────┘               └───────────────────────┘
```

## 2. 패키지 구조

```
com.groom.saga
├── SagaTrackerApplication.kt
│
├── adapter/
│   ├── inbound/
│   │   ├── messaging/
│   │   │   └── SagaTrackerKafkaListener.kt      # Kafka Consumer
│   │   └── web/
│   │       ├── SagaQueryController.kt           # REST API
│   │       └── dto/
│   │           ├── SagaDetailResponse.kt
│   │           ├── SagaListResponse.kt
│   │           └── SagaStepResponse.kt
│   │
│   └── outbound/
│       └── persistence/
│           ├── SagaInstanceJpaEntity.kt
│           ├── SagaStepJpaEntity.kt
│           ├── SagaInstanceJpaRepository.kt
│           ├── SagaStepJpaRepository.kt
│           └── SagaPersistenceAdapter.kt
│
├── application/
│   ├── dto/
│   │   ├── ProcessSagaEventCommand.kt
│   │   ├── GetSagaQuery.kt
│   │   └── SearchSagasQuery.kt
│   ├── service/
│   │   ├── SagaEventProcessor.kt               # Kafka 이벤트 처리
│   │   └── SagaQueryService.kt                 # 조회 서비스
│   └── event/
│       └── SagaProcessedEvent.kt
│
├── common/
│   ├── config/
│   │   └── WebConfig.kt
│   └── exception/
│       ├── SagaNotFoundException.kt
│       └── GlobalExceptionHandler.kt
│
├── configuration/
│   ├── jpa/
│   │   ├── JpaConfig.kt
│   │   └── AuditorAwareConfig.kt
│   └── kafka/
│       ├── KafkaConsumerConfig.kt
│       ├── KafkaConsumerProperties.kt
│       └── KafkaTopicProperties.kt
│
└── domain/
    ├── model/
    │   ├── SagaInstance.kt                     # 도메인 엔티티
    │   ├── SagaStep.kt                         # 도메인 엔티티
    │   ├── SagaType.kt                         # enum
    │   └── SagaStatus.kt                       # enum
    ├── port/
    │   ├── LoadSagaPort.kt                     # 조회 포트
    │   ├── SaveSagaPort.kt                     # 저장 포트
    │   └── SagaMetricsPort.kt                  # 메트릭 포트
    └── service/
        └── SagaAggregator.kt                   # 도메인 서비스
```

## 3. 이벤트 처리 흐름

### 3.1 Saga 이벤트 수신 및 처리

```
1. Kafka 메시지 수신 (SagaTrackerKafkaListener)
   ↓
2. Avro 역직렬화 (SagaTracker → ProcessSagaEventCommand)
   ↓
3. 중복 체크 (eventId 기반)
   ↓ (신규 이벤트인 경우)
4. SagaInstance 조회 또는 생성
   ↓
5. SagaStep 추가
   ↓
6. SagaInstance 상태 업데이트
   ↓
7. 메트릭 갱신
   ↓
8. Commit offset
```

### 3.2 시퀀스 다이어그램

```
┌─────────┐     ┌─────────────┐     ┌────────────────┐     ┌──────────────┐     ┌────────┐
│  Kafka  │     │ KafkaListener│     │ EventProcessor │     │PersistAdapter│     │   DB   │
└────┬────┘     └──────┬──────┘     └───────┬────────┘     └──────┬───────┘     └────┬───┘
     │                 │                    │                     │                  │
     │  SagaTracker    │                    │                     │                  │
     │  message        │                    │                     │                  │
     │────────────────>│                    │                     │                  │
     │                 │                    │                     │                  │
     │                 │ ProcessCommand     │                     │                  │
     │                 │───────────────────>│                     │                  │
     │                 │                    │                     │                  │
     │                 │                    │ findByEventId()     │                  │
     │                 │                    │────────────────────>│                  │
     │                 │                    │                     │    SELECT        │
     │                 │                    │                     │─────────────────>│
     │                 │                    │                     │                  │
     │                 │                    │   Optional.empty()  │                  │
     │                 │                    │<────────────────────│                  │
     │                 │                    │                     │                  │
     │                 │                    │ findBySagaId()      │                  │
     │                 │                    │────────────────────>│                  │
     │                 │                    │                     │                  │
     │                 │                    │   SagaInstance      │                  │
     │                 │                    │<────────────────────│                  │
     │                 │                    │                     │                  │
     │                 │                    │ save(instance,step) │                  │
     │                 │                    │────────────────────>│                  │
     │                 │                    │                     │    INSERT/UPDATE │
     │                 │                    │                     │─────────────────>│
     │                 │                    │                     │                  │
     │                 │   completed        │                     │                  │
     │                 │<───────────────────│                     │                  │
     │                 │                    │                     │                  │
     │   ack           │                    │                     │                  │
     │<────────────────│                    │                     │                  │
     │                 │                    │                     │                  │
```

## 4. 멱등성 보장

### 4.1 중복 이벤트 처리
- **Primary Key**: `eventId` 기반 유니크 제약
- **Upsert 전략**: `sagaId + step + status` 복합 키로 중복 체크

### 4.2 Consumer 설정
```kotlin
// enable-auto-commit: false
// Manual commit after successful processing
// AckMode.MANUAL_IMMEDIATE
```

## 5. 에러 처리

### 5.1 재시도 전략
1. **Transient Error**: Kafka Consumer 자체 재시도 (max 3회)
2. **Persistent Error**: DLQ (`c4ang.saga.tracker.dlt`)로 전송
3. **DLQ 모니터링**: 별도 Consumer로 DLQ 모니터링 및 알림

### 5.2 DLQ 처리 흐름
```
c4ang.saga.tracker
      │
      ▼ (Processing Failed)
c4ang.saga.tracker.dlt
      │
      ▼
DLQ Monitor (Alert to Slack)
      │
      ▼ (Manual Investigation)
Replay to c4ang.saga.tracker
```

## 6. 확장성 고려사항

### 6.1 현재 설계 (v1)
- PostgreSQL 단일 저장소
- Kafka 3 파티션 (sagaId 기반 파티셔닝)
- 단일 Consumer Group

### 6.2 향후 확장 (v2 이후)
- MongoDB 도입 (Document 기반 저장)
- Elasticsearch 연동 (검색 최적화)
- Consumer 스케일 아웃 (3+ instances)
- Redis 캐시 (최근 Saga 조회 성능)
