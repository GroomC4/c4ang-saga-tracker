# Saga Tracker Service - 데이터베이스 스키마

## 1. ERD

```
┌─────────────────────────────────────────────────────────────────┐
│                        saga_instance                             │
├─────────────────────────────────────────────────────────────────┤
│ PK  saga_id          VARCHAR(100)   NOT NULL                    │
│     saga_type        VARCHAR(50)    NOT NULL                    │
│     order_id         VARCHAR(100)   NOT NULL                    │
│     current_status   VARCHAR(20)    NOT NULL                    │
│     last_step        VARCHAR(100)   NULL                        │
│     last_trace_id    VARCHAR(100)   NULL                        │
│     started_at       TIMESTAMP      NOT NULL                    │
│     updated_at       TIMESTAMP      NOT NULL                    │
│     created_at       TIMESTAMP      NOT NULL                    │
├─────────────────────────────────────────────────────────────────┤
│ IDX idx_saga_instance_order_id (order_id)                       │
│ IDX idx_saga_instance_status (current_status)                   │
│ IDX idx_saga_instance_type (saga_type)                          │
│ IDX idx_saga_instance_started_at (started_at)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ 1:N
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          saga_steps                              │
├─────────────────────────────────────────────────────────────────┤
│ PK  id               BIGSERIAL      NOT NULL                    │
│ FK  saga_id          VARCHAR(100)   NOT NULL → saga_instance    │
│     event_id         VARCHAR(100)   NOT NULL  UNIQUE            │
│     step             VARCHAR(100)   NOT NULL                    │
│     status           VARCHAR(20)    NOT NULL                    │
│     producer_service VARCHAR(50)    NULL                        │
│     trace_id         VARCHAR(100)   NULL                        │
│     metadata         JSONB          NULL                        │
│     recorded_at      TIMESTAMP      NOT NULL                    │
│     created_at       TIMESTAMP      NOT NULL                    │
├─────────────────────────────────────────────────────────────────┤
│ IDX idx_saga_steps_saga_id (saga_id)                            │
│ IDX idx_saga_steps_event_id (event_id)                          │
│ IDX idx_saga_steps_step (step)                                  │
│ IDX idx_saga_steps_recorded_at (recorded_at)                    │
│ UNQ uq_saga_steps_event_id (event_id)                           │
└─────────────────────────────────────────────────────────────────┘
```

## 2. DDL

### 2.1 saga_instance 테이블

```sql
CREATE TABLE saga_instance (
    saga_id         VARCHAR(100)    PRIMARY KEY,
    saga_type       VARCHAR(50)     NOT NULL,
    order_id        VARCHAR(100)    NOT NULL,
    current_status  VARCHAR(20)     NOT NULL DEFAULT 'STARTED',
    last_step       VARCHAR(100)    NULL,
    last_trace_id   VARCHAR(100)    NULL,
    started_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_saga_instance_status CHECK (
        current_status IN ('STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'COMPENSATED')
    ),
    CONSTRAINT chk_saga_instance_type CHECK (
        saga_type IN ('ORDER_CREATION', 'PAYMENT_COMPLETION')
    )
);

-- Indexes
CREATE INDEX idx_saga_instance_order_id ON saga_instance(order_id);
CREATE INDEX idx_saga_instance_status ON saga_instance(current_status);
CREATE INDEX idx_saga_instance_type ON saga_instance(saga_type);
CREATE INDEX idx_saga_instance_started_at ON saga_instance(started_at);
CREATE INDEX idx_saga_instance_updated_at ON saga_instance(updated_at);

-- 복합 인덱스: 일반적인 쿼리 패턴 지원
CREATE INDEX idx_saga_instance_type_status ON saga_instance(saga_type, current_status);
CREATE INDEX idx_saga_instance_started_at_status ON saga_instance(started_at DESC, current_status);

COMMENT ON TABLE saga_instance IS 'Saga 인스턴스 메인 테이블';
COMMENT ON COLUMN saga_instance.saga_id IS 'Saga 고유 식별자';
COMMENT ON COLUMN saga_instance.saga_type IS 'Saga 유형 (ORDER_CREATION, PAYMENT_COMPLETION)';
COMMENT ON COLUMN saga_instance.order_id IS '연관된 주문 ID';
COMMENT ON COLUMN saga_instance.current_status IS '현재 Saga 상태';
COMMENT ON COLUMN saga_instance.last_step IS '마지막으로 처리된 단계';
COMMENT ON COLUMN saga_instance.last_trace_id IS '마지막 단계의 트레이스 ID';
```

### 2.2 saga_steps 테이블

```sql
CREATE TABLE saga_steps (
    id               BIGSERIAL       PRIMARY KEY,
    saga_id          VARCHAR(100)    NOT NULL,
    event_id         VARCHAR(100)    NOT NULL UNIQUE,
    step             VARCHAR(100)    NOT NULL,
    status           VARCHAR(20)     NOT NULL,
    producer_service VARCHAR(50)     NULL,
    trace_id         VARCHAR(100)    NULL,
    metadata         JSONB           NULL,
    recorded_at      TIMESTAMP       NOT NULL,
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_saga_steps_saga_id
        FOREIGN KEY (saga_id) REFERENCES saga_instance(saga_id) ON DELETE CASCADE,
    CONSTRAINT chk_saga_steps_status CHECK (
        status IN ('STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'COMPENSATED')
    )
);

-- Indexes
CREATE INDEX idx_saga_steps_saga_id ON saga_steps(saga_id);
CREATE INDEX idx_saga_steps_step ON saga_steps(step);
CREATE INDEX idx_saga_steps_recorded_at ON saga_steps(recorded_at);
CREATE INDEX idx_saga_steps_status ON saga_steps(status);

-- 복합 인덱스
CREATE INDEX idx_saga_steps_saga_step ON saga_steps(saga_id, step);
CREATE INDEX idx_saga_steps_saga_recorded ON saga_steps(saga_id, recorded_at);

-- JSONB GIN 인덱스 (metadata 검색용)
CREATE INDEX idx_saga_steps_metadata ON saga_steps USING GIN (metadata);

COMMENT ON TABLE saga_steps IS 'Saga 단계별 이력 테이블';
COMMENT ON COLUMN saga_steps.event_id IS '이벤트 고유 ID (멱등성 보장)';
COMMENT ON COLUMN saga_steps.step IS 'Saga 단계명 (예: STOCK_RESERVATION, PAYMENT_INITIALIZATION)';
COMMENT ON COLUMN saga_steps.producer_service IS '이벤트 발행 서비스명';
COMMENT ON COLUMN saga_steps.trace_id IS 'OpenTelemetry Trace ID';
COMMENT ON COLUMN saga_steps.metadata IS '추가 메타데이터 (JSON)';
```

## 3. 데이터 정의

### 3.1 Saga Types
| 타입 | 설명 |
|------|------|
| `ORDER_CREATION` | 주문 생성 Saga |
| `PAYMENT_COMPLETION` | 결제 완료 Saga |

### 3.2 Saga Status
| 상태 | 설명 |
|------|------|
| `STARTED` | Saga 시작됨 |
| `IN_PROGRESS` | 진행 중 |
| `COMPLETED` | 정상 완료 |
| `FAILED` | 실패 |
| `COMPENSATED` | 보상 완료 |

### 3.3 Step Names (예시)
| Step | 서비스 | 설명 |
|------|--------|------|
| `ORDER_CREATED` | Order | 주문 생성 |
| `STOCK_RESERVATION` | Product | 재고 예약 |
| `STOCK_RESERVED` | Product | 재고 예약 완료 |
| `PAYMENT_INITIALIZATION` | Payment | 결제 초기화 |
| `PAYMENT_COMPLETED` | Payment | 결제 완료 |
| `ORDER_CONFIRMED` | Order | 주문 확정 |
| `COMPENSATION_STOCK` | Product | 재고 보상 |
| `COMPENSATION_PAYMENT` | Payment | 결제 보상 |

## 4. Metadata JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "traceId": {
      "type": "string",
      "description": "OpenTelemetry Trace ID"
    },
    "spanId": {
      "type": "string",
      "description": "OpenTelemetry Span ID"
    },
    "orderAmount": {
      "type": "number",
      "description": "주문 금액"
    },
    "paymentMethod": {
      "type": "string",
      "description": "결제 방식"
    },
    "failureReason": {
      "type": "string",
      "description": "실패 사유"
    },
    "retryCount": {
      "type": "integer",
      "description": "재시도 횟수"
    },
    "producerTimestamp": {
      "type": "string",
      "format": "date-time",
      "description": "Producer 측 발행 시각"
    }
  }
}
```

## 5. 데이터 보존 정책

### 5.1 Partitioning (향후 도입)
```sql
-- 월별 파티셔닝 예시
CREATE TABLE saga_steps_partitioned (
    LIKE saga_steps INCLUDING ALL
) PARTITION BY RANGE (recorded_at);

CREATE TABLE saga_steps_2024_01 PARTITION OF saga_steps_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

### 5.2 Retention 정책
- **Hot Storage** (90일): PostgreSQL 메인 테이블
- **Cold Storage** (90일+): S3/Glacier 아카이브 (pg_dump → S3)

### 5.3 정리 스케줄러
```sql
-- 90일 이상 된 완료 상태 Saga 삭제 (배치 작업)
DELETE FROM saga_instance
WHERE current_status IN ('COMPLETED', 'COMPENSATED')
  AND updated_at < NOW() - INTERVAL '90 days';
```

## 6. 마이그레이션

### 6.1 Flyway 마이그레이션 파일
```
src/main/resources/db/migration/
├── V1__create_saga_instance_table.sql
├── V2__create_saga_steps_table.sql
├── V3__add_indexes.sql
└── V4__add_metadata_gin_index.sql
```
