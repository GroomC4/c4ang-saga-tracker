# Saga Tracker Service - 모니터링 설계

## 1. 모니터링 아키텍처

```
┌───────────────────────────────────────────────────────────────────────┐
│                        Kubernetes Cluster                              │
│                                                                         │
│  ┌─────────────────────┐     ┌─────────────────────┐                  │
│  │  saga-tracker-api   │     │   Prometheus        │                  │
│  │  ┌───────────────┐  │     │  ┌───────────────┐  │                  │
│  │  │ /actuator     │──┼─────┼─>│ ServiceMonitor│  │                  │
│  │  │ /prometheus   │  │     │  └───────────────┘  │                  │
│  │  └───────────────┘  │     │          │          │                  │
│  │                     │     │          ▼          │                  │
│  │  Micrometer         │     │  ┌───────────────┐  │                  │
│  │  ┌───────────────┐  │     │  │ PrometheusRule│  │                  │
│  │  │ saga_*metrics │  │     │  │ (AlertRules)  │  │                  │
│  │  └───────────────┘  │     │  └───────────────┘  │                  │
│  └─────────────────────┘     └──────────┬──────────┘                  │
│                                         │                              │
│                                         ▼                              │
│                              ┌─────────────────────┐                  │
│                              │      Grafana        │                  │
│                              │  ┌───────────────┐  │                  │
│                              │  │  Dashboard    │  │                  │
│                              │  │  (ConfigMap)  │  │                  │
│                              │  └───────────────┘  │                  │
│                              └──────────┬──────────┘                  │
│                                         │                              │
└─────────────────────────────────────────┼──────────────────────────────┘
                                          │
                              ┌───────────┴───────────┐
                              ▼                       ▼
                      ┌─────────────┐         ┌─────────────┐
                      │    Slack    │         │  PagerDuty  │
                      └─────────────┘         └─────────────┘
```

## 2. Prometheus Metrics

### 2.1 Custom Metrics 정의

```kotlin
// SagaMetrics.kt
@Component
class SagaMetrics(private val meterRegistry: MeterRegistry) {

    // Gauge: 상태별 활성 Saga 수
    private val activeSagaGauge = AtomicLong(0)

    fun updateActiveSagas(status: SagaStatus, count: Long) {
        Gauge.builder("saga_active_total")
            .tag("status", status.name)
            .register(meterRegistry)
    }

    // Counter: 실패한 Saga 수
    private val failedCounter = Counter.builder("saga_failed_total")
        .description("Total number of failed sagas")
        .register(meterRegistry)

    fun incrementFailed() = failedCounter.increment()

    // Counter: 보상 완료된 Saga 수
    private val compensatedCounter = Counter.builder("saga_compensated_total")
        .description("Total number of compensated sagas")
        .register(meterRegistry)

    fun incrementCompensated() = compensatedCounter.increment()

    // Histogram: 보상 소요 시간
    private val compensationTimer = Timer.builder("saga_compensation_duration_seconds")
        .description("Duration from failure to compensation")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry)

    fun recordCompensationDuration(duration: Duration) =
        compensationTimer.record(duration)

    // Counter: 이벤트 처리 수
    private val processedCounter = Counter.builder("saga_events_processed_total")
        .description("Total number of processed saga events")
        .register(meterRegistry)

    fun incrementProcessed(step: String, status: SagaStatus) {
        Counter.builder("saga_events_processed_total")
            .tag("step", step)
            .tag("status", status.name)
            .register(meterRegistry)
            .increment()
    }

    // Gauge: Consumer Lag
    fun updateConsumerLag(lag: Long) {
        Gauge.builder("saga_tracker_consumer_lag")
            .description("Kafka consumer lag")
            .register(meterRegistry)
    }
}
```

### 2.2 메트릭 목록

| 메트릭명 | 타입 | 라벨 | 설명 |
|---------|------|------|------|
| `saga_active_total` | Gauge | status | 상태별 활성 Saga 수 |
| `saga_failed_total` | Counter | - | 실패한 Saga 총 수 |
| `saga_compensated_total` | Counter | - | 보상 완료된 Saga 총 수 |
| `saga_compensation_duration_seconds` | Histogram | - | 보상 소요 시간 |
| `saga_events_processed_total` | Counter | step, status | 처리된 이벤트 수 |
| `saga_tracker_consumer_lag` | Gauge | - | Kafka Consumer Lag |
| `saga_tracker_api_latency_seconds` | Histogram | endpoint, method | API 응답 시간 |
| `saga_tracker_db_query_seconds` | Histogram | query | DB 쿼리 시간 |

## 3. Grafana Dashboard

### 3.1 Dashboard JSON (ConfigMap)

```yaml
# kubernetes/grafana-dashboard-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: saga-tracker-dashboard
  namespace: monitoring
  labels:
    grafana_dashboard: "1"
data:
  saga-tracker-dashboard.json: |
    {
      "dashboard": {
        "title": "Saga Tracker Dashboard",
        "panels": [...]
      }
    }
```

### 3.2 Dashboard 패널 구성

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Saga Tracker Dashboard                            │
├─────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐        │
│ │ Active Sagas    │ │ Failed (24h)    │ │ Compensated     │        │
│ │     125         │ │      12         │ │      8          │        │
│ └─────────────────┘ └─────────────────┘ └─────────────────┘        │
│                                                                     │
│ ┌───────────────────────────────────────────────────────────────┐ │
│ │ Saga Status Distribution (Pie Chart)                           │ │
│ │  [STARTED: 10%] [IN_PROGRESS: 15%] [COMPLETED: 70%]           │ │
│ │  [FAILED: 3%] [COMPENSATED: 2%]                                │ │
│ └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│ ┌───────────────────────────────────────────────────────────────┐ │
│ │ Saga Events Timeline (Time Series)                             │ │
│ │  ──────────────────────────────────────────────────────────── │ │
│ │  │    ∧    ∧         ∧                                        │ │
│ │  │   / \  / \       / \                                       │ │
│ │  │  /   \/   \     /   \                                      │ │
│ │  │ /         \   /     \                                      │ │
│ │  └───────────────────────────────────────────────────────────│ │
│ │    12:00  12:15  12:30  12:45  13:00                          │ │
│ └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│ ┌───────────────────────────────────────────────────────────────┐ │
│ │ Compensation Duration (P95/P99)                                │ │
│ │  P95: 4.2s │ P99: 8.7s                                        │ │
│ └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│ ┌───────────────────────────────────────────────────────────────┐ │
│ │ Consumer Lag                                                   │ │
│ │  Current: 45 messages                                          │ │
│ └───────────────────────────────────────────────────────────────┘ │
│                                                                     │
│ ┌───────────────────────────────────────────────────────────────┐ │
│ │ Recent Failed Sagas (Table)                                    │ │
│ │ SagaID        │ OrderID      │ Last Step     │ Failed At     │ │
│ │ saga-123      │ ORD-001      │ STOCK_RESERVE │ 10:25:00      │ │
│ │ saga-456      │ ORD-002      │ PAYMENT_INIT  │ 10:30:15      │ │
│ └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.3 주요 PromQL 쿼리

```promql
# 활성 Saga 수 (상태별)
saga_active_total

# 5분간 실패율
increase(saga_failed_total[5m])

# 보상 성공률
saga_compensated_total / saga_failed_total

# 보상 소요 시간 P95
histogram_quantile(0.95, rate(saga_compensation_duration_seconds_bucket[5m]))

# Consumer Lag
saga_tracker_consumer_lag

# API 응답 시간 P99
histogram_quantile(0.99, rate(saga_tracker_api_latency_seconds_bucket[5m]))
```

## 4. Alert Rules

### 4.1 PrometheusRule 정의

```yaml
# kubernetes/prometheus-rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: saga-tracker-alerts
  namespace: monitoring
spec:
  groups:
    - name: saga-tracker
      rules:
        # 실패 급증 알림
        - alert: SagaFailureSpike
          expr: increase(saga_failed_total[5m]) > 10
          for: 2m
          labels:
            severity: warning
          annotations:
            summary: "Saga failure spike detected"
            description: "{{ $value }} sagas failed in the last 5 minutes"

        # 보상 지연 알림
        - alert: SagaCompensationTimeout
          expr: |
            (saga_failed_total - saga_compensated_total) > 0
            and on() (time() - saga_last_failed_timestamp) > 300
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "Saga compensation timeout"
            description: "Failed sagas not compensated for more than 5 minutes"

        # Consumer Lag 알림
        - alert: SagaTrackerConsumerLag
          expr: saga_tracker_consumer_lag > 1000
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "High consumer lag detected"
            description: "Consumer lag is {{ $value }} messages"

        # 서비스 다운 알림
        - alert: SagaTrackerDown
          expr: up{job="saga-tracker-api"} == 0
          for: 1m
          labels:
            severity: critical
          annotations:
            summary: "Saga Tracker service is down"
            description: "saga-tracker-api has been down for more than 1 minute"
```

### 4.2 Alert 심각도 정의

| 심각도 | 조건 | 알림 채널 | 대응 |
|--------|------|----------|------|
| `critical` | 서비스 다운, 보상 타임아웃 | PagerDuty + Slack | 즉시 대응 |
| `warning` | 실패 급증, 높은 Lag | Slack | 모니터링 강화 |
| `info` | 이상 징후 감지 | Slack | 상황 인지 |

## 5. Runbook

### 5.1 SagaFailureSpike 대응

```
1. Grafana Dashboard 확인
   - 어떤 Step에서 실패가 발생하는지 확인
   - 특정 서비스에 집중되어 있는지 확인

2. 원인 분석
   GET /api/v1/sagas?status=FAILED&fromDate=<timestamp>
   - metadata에서 failureReason 확인
   - traceId로 Jaeger에서 상세 추적

3. 조치
   - 해당 서비스 로그 확인
   - 필요시 서비스 재시작
   - 인프라 이슈(DB, Kafka) 점검

4. 후속 조치
   - 실패한 Saga 목록 추출
   - 수동 보상 처리 또는 재시도 결정
```

### 5.2 SagaCompensationTimeout 대응

```
1. 미보상 Saga 조회
   SELECT * FROM saga_instance
   WHERE current_status = 'FAILED'
   AND updated_at < NOW() - INTERVAL '5 minutes';

2. 보상 프로세스 확인
   - 관련 도메인 서비스의 보상 로직 정상 동작 확인
   - Kafka 메시지 발행 여부 확인

3. 수동 보상 실행
   PATCH /internal/v1/sagas/{sagaId}/status
   {"status": "COMPENSATED", "reason": "Manual compensation"}

4. 근본 원인 분석
   - 왜 자동 보상이 동작하지 않았는지 분석
   - 필요시 버그 리포트 생성
```

### 5.3 SagaTrackerConsumerLag 대응

```
1. Consumer 상태 확인
   kubectl get pods -l app=saga-tracker-api
   kubectl logs <pod> | grep "consumer"

2. 처리량 분석
   - 초당 메시지 처리량 확인
   - DB 쿼리 지연 확인

3. 스케일 아웃 고려
   kubectl scale deployment saga-tracker-api --replicas=3

4. Kafka 토픽 상태 확인
   kafka-consumer-groups.sh --describe --group saga-tracker-service
```

## 6. ServiceMonitor 설정

```yaml
# kubernetes/service-monitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: saga-tracker-api
  namespace: ecommerce
  labels:
    release: prometheus
spec:
  selector:
    matchLabels:
      app: saga-tracker-api
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
  namespaceSelector:
    matchNames:
      - ecommerce
```
