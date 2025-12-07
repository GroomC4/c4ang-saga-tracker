# Saga Tracker Service - API 명세

## 1. API 개요

| Base URL | Port | Description |
|----------|------|-------------|
| `/api/v1/sagas` | 8086 | Saga 조회 API |
| `/internal/v1/sagas` | 8086 | 내부 서비스용 API |
| `/actuator` | 8086 | 모니터링/헬스체크 |

## 2. Public API

### 2.1 Saga 단건 조회

**GET** `/api/v1/sagas/{sagaId}`

특정 Saga의 상세 정보를 조회합니다.

**Request**
```http
GET /api/v1/sagas/saga-12345 HTTP/1.1
Host: saga-tracker-api:8086
Accept: application/json
```

**Response (200 OK)**
```json
{
  "sagaId": "saga-12345",
  "sagaType": "ORDER_CREATION",
  "orderId": "ORD-2024-001",
  "currentStatus": "COMPLETED",
  "lastStep": "ORDER_CONFIRMED",
  "lastTraceId": "trace-abc123",
  "startedAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:05:30Z",
  "stepCount": 5
}
```

**Response (404 Not Found)**
```json
{
  "error": "NOT_FOUND",
  "message": "Saga not found: saga-12345"
}
```

---

### 2.2 Saga 목록 조회 (검색)

**GET** `/api/v1/sagas`

조건에 맞는 Saga 목록을 조회합니다.

**Query Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orderId` | string | No | 주문 ID로 필터 |
| `sagaType` | string | No | Saga 타입 (ORDER_CREATION, PAYMENT_COMPLETION) |
| `status` | string | No | 상태 (STARTED, IN_PROGRESS, COMPLETED, FAILED, COMPENSATED) |
| `fromDate` | string | No | 시작일 (ISO 8601 format) |
| `toDate` | string | No | 종료일 (ISO 8601 format) |
| `page` | int | No | 페이지 번호 (default: 0) |
| `size` | int | No | 페이지 크기 (default: 20, max: 100) |
| `sort` | string | No | 정렬 (startedAt,desc / updatedAt,asc) |

**Request**
```http
GET /api/v1/sagas?status=FAILED&sagaType=ORDER_CREATION&page=0&size=10 HTTP/1.1
Host: saga-tracker-api:8086
Accept: application/json
```

**Response (200 OK)**
```json
{
  "content": [
    {
      "sagaId": "saga-12345",
      "sagaType": "ORDER_CREATION",
      "orderId": "ORD-2024-001",
      "currentStatus": "FAILED",
      "lastStep": "STOCK_RESERVATION",
      "startedAt": "2024-01-15T10:00:00Z",
      "updatedAt": "2024-01-15T10:02:30Z",
      "stepCount": 2
    }
  ],
  "page": {
    "number": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### 2.3 Saga 단계 조회

**GET** `/api/v1/sagas/{sagaId}/steps`

특정 Saga의 모든 단계를 조회합니다.

**Request**
```http
GET /api/v1/sagas/saga-12345/steps HTTP/1.1
Host: saga-tracker-api:8086
Accept: application/json
```

**Response (200 OK)**
```json
{
  "sagaId": "saga-12345",
  "steps": [
    {
      "id": 1,
      "eventId": "evt-001",
      "step": "ORDER_CREATED",
      "status": "COMPLETED",
      "producerService": "order-service",
      "traceId": "trace-abc123",
      "metadata": {
        "orderAmount": 50000,
        "itemCount": 3
      },
      "recordedAt": "2024-01-15T10:00:00Z"
    },
    {
      "id": 2,
      "eventId": "evt-002",
      "step": "STOCK_RESERVED",
      "status": "COMPLETED",
      "producerService": "product-service",
      "traceId": "trace-abc124",
      "metadata": {},
      "recordedAt": "2024-01-15T10:01:00Z"
    },
    {
      "id": 3,
      "eventId": "evt-003",
      "step": "PAYMENT_COMPLETED",
      "status": "COMPLETED",
      "producerService": "payment-service",
      "traceId": "trace-abc125",
      "metadata": {
        "paymentMethod": "CARD"
      },
      "recordedAt": "2024-01-15T10:03:00Z"
    },
    {
      "id": 4,
      "eventId": "evt-004",
      "step": "ORDER_CONFIRMED",
      "status": "COMPLETED",
      "producerService": "order-service",
      "traceId": "trace-abc126",
      "metadata": {},
      "recordedAt": "2024-01-15T10:05:00Z"
    }
  ]
}
```

---

### 2.4 Saga 통계 조회

**GET** `/api/v1/sagas/statistics`

Saga 상태별 통계를 조회합니다.

**Query Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `sagaType` | string | No | Saga 타입으로 필터 |
| `fromDate` | string | No | 시작일 |
| `toDate` | string | No | 종료일 |

**Request**
```http
GET /api/v1/sagas/statistics?fromDate=2024-01-01&toDate=2024-01-31 HTTP/1.1
Host: saga-tracker-api:8086
Accept: application/json
```

**Response (200 OK)**
```json
{
  "period": {
    "from": "2024-01-01T00:00:00Z",
    "to": "2024-01-31T23:59:59Z"
  },
  "total": 1500,
  "byStatus": {
    "STARTED": 10,
    "IN_PROGRESS": 25,
    "COMPLETED": 1400,
    "FAILED": 50,
    "COMPENSATED": 15
  },
  "byType": {
    "ORDER_CREATION": 1200,
    "PAYMENT_COMPLETION": 300
  },
  "failureRate": 0.0333,
  "compensationRate": 0.30,
  "avgDurationSeconds": 45.2
}
```

---

## 3. Internal API

### 3.1 Saga 상태 강제 업데이트 (관리용)

**PATCH** `/internal/v1/sagas/{sagaId}/status`

관리 목적으로 Saga 상태를 강제 업데이트합니다.

**Request**
```http
PATCH /internal/v1/sagas/saga-12345/status HTTP/1.1
Host: saga-tracker-api:8086
Content-Type: application/json

{
  "status": "COMPENSATED",
  "reason": "Manual compensation after investigation"
}
```

**Response (200 OK)**
```json
{
  "sagaId": "saga-12345",
  "previousStatus": "FAILED",
  "currentStatus": "COMPENSATED",
  "updatedAt": "2024-01-15T15:30:00Z"
}
```

---

### 3.2 DLQ 이벤트 재처리

**POST** `/internal/v1/sagas/replay`

DLQ에 있는 이벤트를 재처리합니다.

**Request**
```http
POST /internal/v1/sagas/replay HTTP/1.1
Host: saga-tracker-api:8086
Content-Type: application/json

{
  "eventIds": ["evt-001", "evt-002"],
  "dryRun": false
}
```

**Response (200 OK)**
```json
{
  "requested": 2,
  "successful": 2,
  "failed": 0,
  "results": [
    {"eventId": "evt-001", "status": "SUCCESS"},
    {"eventId": "evt-002", "status": "SUCCESS"}
  ]
}
```

---

## 4. Actuator Endpoints

### 4.1 Health Check

**GET** `/actuator/health`

```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "kafka": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### 4.2 Prometheus Metrics

**GET** `/actuator/prometheus`

```
# HELP saga_active_total Current active sagas by status
# TYPE saga_active_total gauge
saga_active_total{status="STARTED"} 10
saga_active_total{status="IN_PROGRESS"} 25

# HELP saga_failed_total Total failed sagas
# TYPE saga_failed_total counter
saga_failed_total 150

# HELP saga_compensation_duration_seconds Time taken for compensation
# TYPE saga_compensation_duration_seconds histogram
saga_compensation_duration_seconds_bucket{le="1.0"} 100
saga_compensation_duration_seconds_bucket{le="5.0"} 200
```

---

## 5. Error Responses

### 5.1 표준 에러 형식

```json
{
  "error": "ERROR_CODE",
  "message": "Human readable message",
  "timestamp": "2024-01-15T10:00:00Z",
  "path": "/api/v1/sagas/invalid-id",
  "details": {}
}
```

### 5.2 에러 코드

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | `BAD_REQUEST` | 잘못된 요청 파라미터 |
| 404 | `NOT_FOUND` | 리소스를 찾을 수 없음 |
| 409 | `CONFLICT` | 상태 충돌 (이미 처리됨) |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |
| 503 | `SERVICE_UNAVAILABLE` | 서비스 일시 불가 |

---

## 6. OpenAPI Specification

Swagger UI: `http://saga-tracker-api:8086/swagger-ui`

OpenAPI JSON: `http://saga-tracker-api:8086/api-docs`
