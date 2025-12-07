# Saga Tracker Service - 도메인 서비스 연동 가이드

## 1. 연동 개요

모든 도메인 서비스(Order, Product, Payment, Store)는 Saga 진행 상황을 `c4ang.saga.tracker` 토픽에 발행해야 합니다.

### 1.1 연동 구조

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Domain Service                               │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                    Business Logic                              │ │
│  │  ┌─────────────────┐                                          │ │
│  │  │ Saga Step 처리  │                                          │ │
│  │  │  (성공/실패)    │                                          │ │
│  │  └────────┬────────┘                                          │ │
│  │           │                                                    │ │
│  │           ▼                                                    │ │
│  │  ┌─────────────────┐                                          │ │
│  │  │SagaTrackerClient│  ← SDK (공통 라이브러리)                 │ │
│  │  │ .recordStep()   │                                          │ │
│  │  └────────┬────────┘                                          │ │
│  └───────────┼───────────────────────────────────────────────────┘ │
│              │                                                       │
│              ▼                                                       │
│  ┌─────────────────────┐                                            │
│  │   Kafka Template    │                                            │
│  │  (Avro Serializer)  │                                            │
│  └──────────┬──────────┘                                            │
└─────────────┼────────────────────────────────────────────────────────┘
              │
              ▼
     ┌─────────────────────┐
     │  c4ang.saga.tracker │
     │      (Kafka)        │
     └─────────────────────┘
```

## 2. SDK 설계

### 2.1 SagaTrackerClient 인터페이스

```kotlin
// platform-core 또는 contract-hub에 추가될 공통 SDK

interface SagaTrackerClient {
    /**
     * Saga 단계를 기록합니다.
     *
     * @param sagaId Saga 고유 ID (일반적으로 orderId)
     * @param sagaType Saga 유형
     * @param step 현재 단계명
     * @param status 상태
     * @param orderId 연관 주문 ID
     * @param metadata 추가 메타데이터 (JSON)
     */
    fun recordStep(
        sagaId: String,
        sagaType: SagaType,
        step: String,
        status: SagaStatus,
        orderId: String,
        metadata: Map<String, Any>? = null
    )
}
```

### 2.2 SagaTrackerClient 구현체

```kotlin
@Component
class KafkaSagaTrackerClient(
    private val kafkaTemplate: KafkaTemplate<String, SagaTracker>,
    private val topicProperties: KafkaTopicProperties,
    private val tracer: Tracer? = null  // OpenTelemetry (Optional)
) : SagaTrackerClient {

    private val logger = KotlinLogging.logger {}

    override fun recordStep(
        sagaId: String,
        sagaType: SagaType,
        step: String,
        status: SagaStatus,
        orderId: String,
        metadata: Map<String, Any>?
    ) {
        val eventId = UUID.randomUUID().toString()
        val now = Instant.now()

        // OpenTelemetry Trace ID 주입
        val enrichedMetadata = buildMetadata(metadata)

        val event = SagaTracker.newBuilder()
            .setEventId(eventId)
            .setEventTimestamp(now.toEpochMilli())
            .setSagaId(sagaId)
            .setSagaType(sagaType.toAvro())
            .setStep(step)
            .setStatus(status.toAvro())
            .setOrderId(orderId)
            .setMetadata(objectMapper.writeValueAsString(enrichedMetadata))
            .setRecordedAt(now.toEpochMilli())
            .build()

        kafkaTemplate.send(topicProperties.sagaTracker, sagaId, event)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error(ex) { "Failed to send saga tracker event: $eventId" }
                } else {
                    logger.debug { "Saga tracker event sent: $eventId, offset: ${result.recordMetadata.offset()}" }
                }
            }
    }

    private fun buildMetadata(metadata: Map<String, Any>?): Map<String, Any> {
        val result = metadata?.toMutableMap() ?: mutableMapOf()

        // OpenTelemetry Trace/Span ID 자동 주입
        tracer?.currentSpan()?.let { span ->
            result["traceId"] = span.spanContext.traceId
            result["spanId"] = span.spanContext.spanId
        }

        result["producerTimestamp"] = Instant.now().toString()

        return result
    }
}
```

### 2.3 Step 네이밍 컨벤션

```kotlin
object SagaSteps {
    // Order Service
    const val ORDER_CREATED = "ORDER_CREATED"
    const val ORDER_CONFIRMED = "ORDER_CONFIRMED"
    const val ORDER_CANCELLED = "ORDER_CANCELLED"
    const val ORDER_TIMEOUT = "ORDER_TIMEOUT"

    // Product Service
    const val STOCK_RESERVATION = "STOCK_RESERVATION"
    const val STOCK_RESERVED = "STOCK_RESERVED"
    const val STOCK_RESERVATION_FAILED = "STOCK_RESERVATION_FAILED"
    const val STOCK_RELEASED = "STOCK_RELEASED"

    // Payment Service
    const val PAYMENT_INITIALIZATION = "PAYMENT_INITIALIZATION"
    const val PAYMENT_INITIALIZED = "PAYMENT_INITIALIZED"
    const val PAYMENT_COMPLETED = "PAYMENT_COMPLETED"
    const val PAYMENT_FAILED = "PAYMENT_FAILED"
    const val PAYMENT_CANCELLED = "PAYMENT_CANCELLED"
    const val PAYMENT_REFUNDED = "PAYMENT_REFUNDED"

    // Compensation
    const val COMPENSATION_STOCK = "COMPENSATION_STOCK"
    const val COMPENSATION_PAYMENT = "COMPENSATION_PAYMENT"
}
```

## 3. 서비스별 연동 작업

### 3.1 Order Service (c4ang-order-service)

#### 연동 포인트

| 위치 | Step | Status | 설명 |
|------|------|--------|------|
| `CreateOrderService` | ORDER_CREATED | STARTED | 주문 생성 시 |
| `StockReservedEventHandler` | STOCK_RESERVED | IN_PROGRESS | 재고 예약 완료 수신 |
| `PaymentCompletedEventHandler` | PAYMENT_COMPLETED | IN_PROGRESS | 결제 완료 수신 |
| `OrderConfirmedEventHandler` | ORDER_CONFIRMED | COMPLETED | 주문 확정 |
| `OrderCancelledEventHandler` | ORDER_CANCELLED | FAILED | 주문 취소 |
| `OrderTimeoutEventHandler` | ORDER_TIMEOUT | FAILED | 주문 타임아웃 |

#### 코드 예시

```kotlin
// CreateOrderService.kt
@Service
class CreateOrderService(
    private val sagaTrackerClient: SagaTrackerClient,
    // ... 기존 의존성
) {
    @Transactional
    fun createOrder(command: CreateOrderCommand): CreateOrderResult {
        // 1. 주문 생성 로직
        val order = orderManager.createOrder(command)
        saveOrderPort.save(order)

        // 2. Saga Tracker 기록
        sagaTrackerClient.recordStep(
            sagaId = order.id.toString(),
            sagaType = SagaType.ORDER_CREATION,
            step = SagaSteps.ORDER_CREATED,
            status = SagaStatus.STARTED,
            orderId = order.orderNumber,
            metadata = mapOf(
                "customerId" to command.customerId,
                "totalAmount" to order.totalAmount,
                "itemCount" to order.items.size
            )
        )

        // 3. 도메인 이벤트 발행
        domainEventPublisher.publish(OrderCreatedEvent(order))

        return CreateOrderResult.from(order)
    }
}
```

### 3.2 Product Service (c4ang-product-service)

#### 연동 포인트

| 위치 | Step | Status | 설명 |
|------|------|--------|------|
| `StockReservationService` | STOCK_RESERVATION | IN_PROGRESS | 재고 예약 시작 |
| `StockReservationService` | STOCK_RESERVED | COMPLETED | 재고 예약 성공 |
| `StockReservationService` | STOCK_RESERVATION_FAILED | FAILED | 재고 부족 |
| `StockReleaseService` | STOCK_RELEASED | COMPENSATED | 재고 해제 (보상) |

#### 코드 예시

```kotlin
// StockReservationService.kt
@Service
class StockReservationService(
    private val sagaTrackerClient: SagaTrackerClient,
    // ... 기존 의존성
) {
    @Transactional
    fun reserveStock(command: ReserveStockCommand): ReserveStockResult {
        val orderId = command.orderId

        // 1. Saga Tracker 기록 (시작)
        sagaTrackerClient.recordStep(
            sagaId = orderId,
            sagaType = SagaType.ORDER_CREATION,
            step = SagaSteps.STOCK_RESERVATION,
            status = SagaStatus.IN_PROGRESS,
            orderId = orderId
        )

        try {
            // 2. 재고 예약 로직
            val result = stockManager.reserve(command)

            // 3. Saga Tracker 기록 (성공)
            sagaTrackerClient.recordStep(
                sagaId = orderId,
                sagaType = SagaType.ORDER_CREATION,
                step = SagaSteps.STOCK_RESERVED,
                status = SagaStatus.COMPLETED,
                orderId = orderId,
                metadata = mapOf("reservedItems" to result.items.size)
            )

            return result
        } catch (e: InsufficientStockException) {
            // 4. Saga Tracker 기록 (실패)
            sagaTrackerClient.recordStep(
                sagaId = orderId,
                sagaType = SagaType.ORDER_CREATION,
                step = SagaSteps.STOCK_RESERVATION_FAILED,
                status = SagaStatus.FAILED,
                orderId = orderId,
                metadata = mapOf("failureReason" to e.message)
            )
            throw e
        }
    }
}
```

### 3.3 Payment Service (c4ang-payment-service)

#### 연동 포인트

| 위치 | Step | Status | 설명 |
|------|------|--------|------|
| `PaymentInitializationService` | PAYMENT_INITIALIZATION | IN_PROGRESS | 결제 초기화 |
| `PaymentInitializationService` | PAYMENT_INITIALIZED | IN_PROGRESS | 결제 초기화 완료 |
| `PaymentCompletionService` | PAYMENT_COMPLETED | COMPLETED | 결제 완료 |
| `PaymentCompletionService` | PAYMENT_FAILED | FAILED | 결제 실패 |
| `PaymentCancellationService` | PAYMENT_CANCELLED | COMPENSATED | 결제 취소 (보상) |
| `RefundService` | PAYMENT_REFUNDED | COMPENSATED | 환불 완료 |

### 3.4 Store Service (c4ang-store-service)

Store Service는 현재 Saga 참여가 없으나, 향후 스토어 관련 Saga 추가 시 동일 패턴 적용.

## 4. Contract Hub 변경사항

### 4.1 SagaType enum 확장

```avro
// src/main/events/avro/saga/SagaTracker.avsc
{
  "name": "sagaType",
  "type": {
    "type": "enum",
    "name": "SagaType",
    "symbols": [
      "ORDER_CREATION",
      "PAYMENT_COMPLETION",
      "REFUND",            // 추가 예정
      "STORE_REGISTRATION" // 추가 예정
    ]
  }
}
```

### 4.2 SDK 패키지 추가

```kotlin
// c4ang-contract-hub 또는 c4ang-platform-core에 추가
// src/main/kotlin/com/groom/saga/client/
├── SagaTrackerClient.kt
├── KafkaSagaTrackerClient.kt
├── SagaSteps.kt
└── SagaTrackerAutoConfiguration.kt
```

## 5. 의존성 추가

각 도메인 서비스의 `build.gradle.kts`에 추가:

```kotlin
dependencies {
    // Saga Tracker SDK (platform-core에 포함되거나 별도 모듈)
    implementation("io.github.groomc4:saga-tracker-sdk:${platformCoreVersion}")
}
```

## 6. 설정 추가

각 도메인 서비스의 `application.yml`에 추가:

```yaml
kafka:
  topics:
    saga-tracker: c4ang.saga.tracker

# Saga Tracker 설정
saga:
  tracker:
    enabled: true
    async: true  # 비동기 발행 (기본값)
```

## 7. 테스트 전략

### 7.1 단위 테스트

```kotlin
class CreateOrderServiceTest {
    @MockK
    lateinit var sagaTrackerClient: SagaTrackerClient

    @Test
    fun `주문 생성 시 Saga Tracker에 기록한다`() {
        // given
        val command = CreateOrderCommand(...)

        // when
        service.createOrder(command)

        // then
        verify {
            sagaTrackerClient.recordStep(
                sagaId = any(),
                sagaType = SagaType.ORDER_CREATION,
                step = SagaSteps.ORDER_CREATED,
                status = SagaStatus.STARTED,
                orderId = any(),
                metadata = any()
            )
        }
    }
}
```

### 7.2 통합 테스트

```kotlin
@SpringBootTest
@EmbeddedKafka
class SagaTrackerIntegrationTest {

    @Test
    fun `전체 Saga 흐름을 추적한다`() {
        // given: 주문 생성 요청

        // when: 주문 생성 API 호출

        // then: saga.tracker 토픽에 메시지 발행 확인
        val records = KafkaTestUtils.getRecords(consumer)
        assertThat(records).hasSize(1)

        val trackerEvent = records.first().value() as SagaTracker
        assertThat(trackerEvent.step).isEqualTo("ORDER_CREATED")
        assertThat(trackerEvent.status).isEqualTo(SagaStatus.STARTED)
    }
}
```

## 8. 마이그레이션 체크리스트

### Order Service
- [ ] SagaTrackerClient 의존성 추가
- [ ] CreateOrderService에 recordStep 호출 추가
- [ ] 각 EventHandler에 recordStep 호출 추가
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성

### Product Service
- [ ] SagaTrackerClient 의존성 추가
- [ ] StockReservationService에 recordStep 호출 추가
- [ ] StockReleaseService에 recordStep 호출 추가
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성

### Payment Service
- [ ] SagaTrackerClient 의존성 추가
- [ ] PaymentInitializationService에 recordStep 호출 추가
- [ ] PaymentCompletionService에 recordStep 호출 추가
- [ ] PaymentCancellationService에 recordStep 호출 추가
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
