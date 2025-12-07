-- V3__add_indexes.sql
-- 인덱스 추가

-- saga_instance 인덱스
CREATE INDEX idx_saga_instance_order_id ON saga_instance(order_id);
CREATE INDEX idx_saga_instance_status ON saga_instance(current_status);
CREATE INDEX idx_saga_instance_type ON saga_instance(saga_type);
CREATE INDEX idx_saga_instance_started_at ON saga_instance(started_at);
CREATE INDEX idx_saga_instance_updated_at ON saga_instance(updated_at);

-- 복합 인덱스: 일반적인 쿼리 패턴 지원
CREATE INDEX idx_saga_instance_type_status ON saga_instance(saga_type, current_status);
CREATE INDEX idx_saga_instance_started_at_status ON saga_instance(started_at DESC, current_status);

-- saga_steps 인덱스
CREATE INDEX idx_saga_steps_saga_id ON saga_steps(saga_id);
CREATE INDEX idx_saga_steps_step ON saga_steps(step);
CREATE INDEX idx_saga_steps_recorded_at ON saga_steps(recorded_at);
CREATE INDEX idx_saga_steps_status ON saga_steps(status);

-- 복합 인덱스
CREATE INDEX idx_saga_steps_saga_step ON saga_steps(saga_id, step);
CREATE INDEX idx_saga_steps_saga_recorded ON saga_steps(saga_id, recorded_at);
