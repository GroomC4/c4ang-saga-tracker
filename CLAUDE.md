# Saga Tracker Service

Saga 추적 및 모니터링 서비스. Kafka 이벤트 기반 중앙 Saga 히스토리 관리.

## Quick Start
```bash
./gradlew :saga-tracker-api:build      # 빌드
./gradlew :saga-tracker-api:test       # 테스트
./gradlew :saga-tracker-api:bootRun    # 실행 (port: 8086)
```

## 아키텍처
- Hexagonal Architecture (Ports & Adapters)
- Kafka Consumer: `c4ang.saga.tracker` 토픽 구독
- PostgreSQL: saga_instance, saga_steps 테이블

## 핵심 문서
- `docs/01-PROJECT-OVERVIEW.md` - 프로젝트 개요
- `docs/02-ARCHITECTURE.md` - 아키텍처 설계
- `docs/03-DATABASE-SCHEMA.md` - DB 스키마
- `docs/04-API-SPECIFICATION.md` - API 명세
- `docs/05-MONITORING.md` - 모니터링 설계
- `docs/06-DOMAIN-SERVICE-INTEGRATION.md` - 도메인 서비스 연동 가이드
- `docs/TODO.md` - 작업 목록

## 기술 스택
Kotlin 2.0 | JDK 21 | Spring Boot 3.3.4 | Gradle Kotlin DSL | Kafka+Avro | PostgreSQL

## 관련 레포
- `c4ang-contract-hub` - Avro 스키마 (SagaTracker.avsc)
- `c4ang-platform-core` - 공통 라이브러리 (SagaTrackerClient SDK)
- `c4ang-order-service` - Order Saga Producer
- `c4ang-product-service` - Stock Saga Producer
- `c4ang-payment-service` - Payment Saga Producer
