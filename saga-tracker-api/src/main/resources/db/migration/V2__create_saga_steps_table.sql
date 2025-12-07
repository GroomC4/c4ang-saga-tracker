-- V2__create_saga_steps_table.sql
-- Saga 단계별 이력 테이블 생성

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

COMMENT ON TABLE saga_steps IS 'Saga 단계별 이력 테이블';
COMMENT ON COLUMN saga_steps.event_id IS '이벤트 고유 ID (멱등성 보장)';
COMMENT ON COLUMN saga_steps.step IS 'Saga 단계명 (예: STOCK_RESERVATION, PAYMENT_INITIALIZATION)';
COMMENT ON COLUMN saga_steps.producer_service IS '이벤트 발행 서비스명';
COMMENT ON COLUMN saga_steps.trace_id IS 'OpenTelemetry Trace ID';
COMMENT ON COLUMN saga_steps.metadata IS '추가 메타데이터 (JSON)';
