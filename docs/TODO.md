# Saga Tracker Service - TODO

## 작업 순서 개요

```
Phase 1: Saga Tracker Service 구현
    ↓
Phase 2: Infrastructure 구성
    ↓
Phase 3: SDK 개발 (Contract Hub / Platform Core)
    ↓
Phase 4: 테스트 및 검증
    ↓
Phase 5: 도메인 서비스 연동 (마지막)
```

---

## Phase 1: Saga Tracker Service 핵심 구현

### 1.1 Domain Layer
- [ ] `SagaInstance` 도메인 엔티티 구현
- [ ] `SagaStep` 도메인 엔티티 구현
- [ ] `SagaType` enum 정의
- [ ] `SagaStatus` enum 정의
- [ ] `LoadSagaPort` 인터페이스 정의
- [ ] `SaveSagaPort` 인터페이스 정의
- [ ] `SagaMetricsPort` 인터페이스 정의
- [ ] `SagaAggregator` 도메인 서비스 구현

### 1.2 Application Layer
- [ ] `ProcessSagaEventCommand` DTO 구현
- [ ] `GetSagaQuery` DTO 구현
- [ ] `SearchSagasQuery` DTO 구현
- [ ] `SagaEventProcessor` 서비스 구현 (Kafka 이벤트 처리)
- [ ] `SagaQueryService` 서비스 구현 (조회 로직)

### 1.3 Adapter - Inbound
- [ ] `SagaTrackerKafkaListener` 구현 (Kafka Consumer)
- [ ] `SagaQueryController` 구현 (REST API)
- [ ] Request/Response DTO 구현
  - [ ] `SagaDetailResponse`
  - [ ] `SagaListResponse`
  - [ ] `SagaStepResponse`
  - [ ] `SagaStatisticsResponse`

### 1.4 Adapter - Outbound
- [ ] `SagaInstanceJpaEntity` 구현
- [ ] `SagaStepJpaEntity` 구현
- [ ] `SagaInstanceJpaRepository` 구현
- [ ] `SagaStepJpaRepository` 구현
- [ ] `SagaPersistenceAdapter` 구현 (Port 구현체)

### 1.5 Configuration
- [ ] `KafkaConsumerConfig` 구현
- [ ] `KafkaConsumerProperties` 구현
- [ ] `KafkaTopicProperties` 구현
- [ ] `JpaConfig` 구현
- [ ] `AuditorAwareConfig` 구현
- [ ] `WebConfig` 구현

### 1.6 Common
- [ ] `GlobalExceptionHandler` 구현
- [ ] `SagaNotFoundException` 예외 클래스
- [ ] `DuplicateEventException` 예외 클래스

### 1.7 Metrics
- [ ] `SagaMetrics` 클래스 구현 (Micrometer)
  - [ ] `saga_active_total` Gauge
  - [ ] `saga_failed_total` Counter
  - [ ] `saga_compensated_total` Counter
  - [ ] `saga_compensation_duration_seconds` Histogram
  - [ ] `saga_events_processed_total` Counter
  - [ ] `saga_tracker_consumer_lag` Gauge

---

## Phase 2: Infrastructure 구성

### 2.1 Database
- [ ] Flyway 마이그레이션 설정
- [ ] `V1__create_saga_instance_table.sql` 작성
- [ ] `V2__create_saga_steps_table.sql` 작성
- [ ] `V3__add_indexes.sql` 작성
- [ ] `V4__add_metadata_gin_index.sql` 작성
- [ ] 로컬 PostgreSQL Docker 설정

### 2.2 Kafka
- [ ] `c4ang.saga.tracker` 토픽 생성 스크립트
- [ ] `c4ang.saga.tracker.dlt` DLQ 토픽 생성 스크립트
- [ ] 로컬 Kafka Docker Compose 설정

### 2.3 Kubernetes (c4ang-infra)
- [ ] Deployment YAML 작성
- [ ] Service YAML 작성
- [ ] ConfigMap YAML 작성
- [ ] Secret YAML 작성
- [ ] HPA (Horizontal Pod Autoscaler) 설정
- [ ] Helm Chart 작성 (선택)

### 2.4 Monitoring (c4ang-infra)
- [ ] ServiceMonitor 설정
- [ ] PrometheusRule (AlertRules) 설정
- [ ] Grafana Dashboard ConfigMap 작성
- [ ] Slack/PagerDuty 알림 연동

### 2.5 CI/CD
- [ ] GitHub Actions workflow 작성
- [ ] Docker 이미지 빌드 설정
- [ ] ECR 푸시 설정
- [ ] ArgoCD Application 설정

---

## Phase 3: SDK 개발

### 3.1 Contract Hub 업데이트 (c4ang-contract-hub)
- [ ] `SagaTracker.avsc` 검토 및 필요시 업데이트
- [ ] SagaType enum 확장 검토
- [ ] SDK 패키지 추가 위치 결정 (contract-hub vs platform-core)

### 3.2 SagaTrackerClient SDK (c4ang-platform-core)
- [ ] `SagaTrackerClient` 인터페이스 정의
- [ ] `KafkaSagaTrackerClient` 구현체 작성
- [ ] `SagaSteps` 상수 클래스 작성
- [ ] `SagaTrackerAutoConfiguration` 작성
- [ ] OpenTelemetry Trace ID 자동 주입 로직
- [ ] 비동기/동기 발행 옵션 지원
- [ ] SDK 단위 테스트 작성
- [ ] SDK 버전 배포 (GitHub Packages)

### 3.3 SDK 문서화
- [ ] SDK 사용 가이드 작성
- [ ] Step 네이밍 컨벤션 문서화
- [ ] Metadata 스키마 문서화

---

## Phase 4: 테스트 및 검증

### 4.1 Unit Tests
- [ ] Domain 계층 테스트
- [ ] Application 계층 테스트
- [ ] Adapter 계층 테스트

### 4.2 Integration Tests
- [ ] Kafka Consumer 통합 테스트 (Testcontainers)
- [ ] JPA Repository 통합 테스트 (Testcontainers)
- [ ] REST API 통합 테스트

### 4.3 E2E Tests (c4ang-quality-gate)
- [ ] Saga Tracker 관련 E2E 테스트 시나리오 작성
- [ ] 전체 Saga 흐름 검증 테스트

### 4.4 로컬 검증
- [ ] Docker Compose로 전체 인프라 기동
- [ ] 수동 Kafka 메시지 발행 테스트
- [ ] API 호출 테스트 (Postman/curl)
- [ ] Prometheus 메트릭 확인
- [ ] Grafana 대시보드 확인

### 4.5 Staging 환경 검증
- [ ] K8s dev 환경 배포
- [ ] Smoke 테스트
- [ ] 모니터링/알림 동작 확인

---

## Phase 5: 도메인 서비스 연동 (마지막)

> **중요**: 이 Phase는 Saga Tracker Service가 완전히 구현되고 검증된 후에 진행합니다.

### SDK 설정 가이드

Saga Tracker SDK는 `c4ang-platform-core` (v2.5.0+)에 포함되어 있습니다.

#### 필수 설정 (application.yml 또는 application-{profile}.yml)

```yaml
platform:
  saga:
    enabled: true                    # 기본값: false (비활성화)
    topic: c4ang.saga.tracker        # 기본값: c4ang.saga.tracker
```

> **⚠️ 중요**: `platform.saga.enabled=true`를 명시적으로 설정해야 SDK가 활성화됩니다!
> - `enabled: false` 또는 미설정 → `NoOpSagaTrackerClient` 사용 (아무 동작 안함)
> - `enabled: true` → `KafkaSagaTrackerClient` 사용 (Kafka로 이벤트 발행)

#### SDK 사용 예시

```kotlin
@Service
class OrderService(
    private val sagaTrackerClient: SagaTrackerClient
) {
    fun createOrder(orderId: String, sagaId: String) {
        // 주문 생성 로직...

        sagaTrackerClient.recordStart(
            sagaId = sagaId,
            sagaType = SagaType.ORDER_CREATION,
            step = SagaSteps.ORDER_CREATED,
            orderId = orderId,
            metadata = mapOf("customerId" to customerId)
        )
    }
}
```

#### 환경별 권장 설정

| 환경 | `platform.saga.enabled` | 설명 |
|------|------------------------|------|
| local | `false` | 로컬 개발 시 Saga Tracker 불필요 |
| dev | `true` | 개발 환경에서 테스트 |
| staging | `true` | 스테이징 검증 |
| prod | `true` | 프로덕션 운영 |

---

### 5.1 Order Service (c4ang-order-service)
- [ ] `application.yml`에 saga 설정 추가 (`platform.saga.enabled: true`)
- [ ] `CreateOrderService`에 Saga Tracker 기록 추가
  - Step: `ORDER_CREATED`, Status: `STARTED`
- [ ] `StockReservedEventHandler`에 기록 추가
  - Step: `STOCK_RESERVED`, Status: `IN_PROGRESS`
- [ ] `PaymentCompletedEventHandler`에 기록 추가
  - Step: `PAYMENT_COMPLETED`, Status: `IN_PROGRESS`
- [ ] `OrderConfirmedEventHandler`에 기록 추가
  - Step: `ORDER_CONFIRMED`, Status: `COMPLETED`
- [ ] `OrderCancelledEventHandler`에 기록 추가
  - Step: `ORDER_CANCELLED`, Status: `FAILED`
- [ ] `OrderTimeoutEventHandler`에 기록 추가
  - Step: `ORDER_TIMEOUT`, Status: `FAILED`
- [ ] 단위 테스트 추가/수정
- [ ] 통합 테스트 추가/수정

### 5.2 Product Service (c4ang-product-service)
- [ ] `application.yml`에 saga 설정 추가 (`platform.saga.enabled: true`)
- [ ] `StockReservationService`에 Saga Tracker 기록 추가
  - Step: `STOCK_RESERVATION`, Status: `IN_PROGRESS`
  - Step: `STOCK_RESERVED`, Status: `COMPLETED` (성공 시)
  - Step: `STOCK_RESERVATION_FAILED`, Status: `FAILED` (실패 시)
- [ ] `StockReleaseService`에 기록 추가 (보상)
  - Step: `STOCK_RELEASED`, Status: `COMPENSATED`
- [ ] 단위 테스트 추가/수정
- [ ] 통합 테스트 추가/수정

### 5.3 Payment Service (c4ang-payment-service)
- [ ] `application.yml`에 saga 설정 추가 (`platform.saga.enabled: true`)
- [ ] `PaymentInitializationService`에 기록 추가
  - Step: `PAYMENT_INITIALIZATION`, Status: `IN_PROGRESS`
  - Step: `PAYMENT_INITIALIZED`, Status: `IN_PROGRESS`
- [ ] `PaymentCompletionService`에 기록 추가
  - Step: `PAYMENT_COMPLETED`, Status: `COMPLETED`
  - Step: `PAYMENT_FAILED`, Status: `FAILED`
- [ ] `PaymentCancellationService`에 기록 추가
  - Step: `PAYMENT_CANCELLED`, Status: `COMPENSATED`
- [ ] `RefundService`에 기록 추가
  - Step: `PAYMENT_REFUNDED`, Status: `COMPENSATED`
- [ ] 단위 테스트 추가/수정
- [ ] 통합 테스트 추가/수정

### 5.4 Store Service (c4ang-store-service)
- [ ] (향후 Saga 참여 시) SDK 연동 검토

### 5.5 통합 검증
- [ ] 전체 Order Creation Saga 흐름 E2E 테스트
- [ ] 실패 시나리오 테스트 (재고 부족, 결제 실패)
- [ ] 보상 트랜잭션 흐름 테스트
- [ ] Grafana Dashboard에서 전체 흐름 모니터링 확인

---

## 우선순위 및 의존관계

```
[높음] Phase 1.1-1.4 → Phase 1.5-1.7 → Phase 2.1-2.2
          ↓
[높음] Phase 3 (SDK 개발)
          ↓
[중간] Phase 2.3-2.5 (K8s/CI 구성)
          ↓
[중간] Phase 4 (테스트)
          ↓
[낮음] Phase 5 (도메인 서비스 연동) ← 모든 선행 작업 완료 후
```

---

## 체크포인트

### Milestone 1: MVP 완료
- [ ] Kafka Consumer 동작
- [ ] DB 저장 동작
- [ ] REST API 동작
- [ ] 기본 메트릭 노출

### Milestone 2: 운영 준비 완료
- [ ] K8s 배포 완료
- [ ] 모니터링/알림 설정 완료
- [ ] SDK 배포 완료

### Milestone 3: 전체 연동 완료
- [ ] 모든 도메인 서비스 연동 완료
- [ ] E2E 테스트 통과
- [ ] 프로덕션 배포 완료

---

## 참고 문서

- [01-PROJECT-OVERVIEW.md](./01-PROJECT-OVERVIEW.md)
- [02-ARCHITECTURE.md](./02-ARCHITECTURE.md)
- [03-DATABASE-SCHEMA.md](./03-DATABASE-SCHEMA.md)
- [04-API-SPECIFICATION.md](./04-API-SPECIFICATION.md)
- [05-MONITORING.md](./05-MONITORING.md)
- [06-DOMAIN-SERVICE-INTEGRATION.md](./06-DOMAIN-SERVICE-INTEGRATION.md)
- [c4ang-contract-hub/docs/guide/saga-tracker-data-recording-strategy.md](../../c4ang-contract-hub/docs/guide/saga-tracker-data-recording-strategy.md)
- [c4ang-contract-hub/docs/guide/saga-tracker-implementation-plan.md](../../c4ang-contract-hub/docs/guide/saga-tracker-implementation-plan.md)
