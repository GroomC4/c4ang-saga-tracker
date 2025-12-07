-- V1__create_saga_instance_table.sql
-- Saga 인스턴스 메인 테이블 생성

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

COMMENT ON TABLE saga_instance IS 'Saga 인스턴스 메인 테이블';
COMMENT ON COLUMN saga_instance.saga_id IS 'Saga 고유 식별자';
COMMENT ON COLUMN saga_instance.saga_type IS 'Saga 유형 (ORDER_CREATION, PAYMENT_COMPLETION)';
COMMENT ON COLUMN saga_instance.order_id IS '연관된 주문 ID';
COMMENT ON COLUMN saga_instance.current_status IS '현재 Saga 상태';
COMMENT ON COLUMN saga_instance.last_step IS '마지막으로 처리된 단계';
COMMENT ON COLUMN saga_instance.last_trace_id IS '마지막 단계의 트레이스 ID';
