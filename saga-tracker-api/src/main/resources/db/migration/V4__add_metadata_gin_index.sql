-- V4__add_metadata_gin_index.sql
-- JSONB GIN 인덱스 추가 (metadata 검색용)

CREATE INDEX idx_saga_steps_metadata ON saga_steps USING GIN (metadata);
