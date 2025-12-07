package com.groom.saga.adapter.outbound.metrics

import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.port.SagaMetricsPort
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Component
class SagaMetricsAdapter(
    private val meterRegistry: MeterRegistry
) : SagaMetricsPort {

    private val activeSagaCounts = ConcurrentHashMap<SagaStatus, AtomicLong>()
    private val consumerLag = AtomicLong(0)

    private val failedCounter: Counter = Counter.builder("saga_failed_total")
        .description("Total number of failed sagas")
        .register(meterRegistry)

    private val compensatedCounter: Counter = Counter.builder("saga_compensated_total")
        .description("Total number of compensated sagas")
        .register(meterRegistry)

    private val compensationTimer: Timer = Timer.builder("saga_compensation_duration_seconds")
        .description("Duration from failure to compensation")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry)

    init {
        // 각 상태별 Gauge 등록
        SagaStatus.entries.forEach { status ->
            val count = AtomicLong(0)
            activeSagaCounts[status] = count
            Gauge.builder("saga_active_total") { count.get().toDouble() }
                .tag("status", status.name)
                .description("Current active sagas by status")
                .register(meterRegistry)
        }

        // Consumer Lag Gauge
        Gauge.builder("saga_tracker_consumer_lag") { consumerLag.get().toDouble() }
            .description("Kafka consumer lag")
            .register(meterRegistry)
    }

    override fun incrementProcessedEvents(step: String, status: SagaStatus) {
        Counter.builder("saga_events_processed_total")
            .tag("step", step)
            .tag("status", status.name)
            .description("Total number of processed saga events")
            .register(meterRegistry)
            .increment()
    }

    override fun incrementFailed() {
        failedCounter.increment()
    }

    override fun incrementCompensated() {
        compensatedCounter.increment()
    }

    override fun recordCompensationDuration(duration: Duration) {
        compensationTimer.record(duration)
    }

    override fun updateActiveSagas(status: SagaStatus, count: Long) {
        activeSagaCounts[status]?.set(count)
    }

    override fun updateConsumerLag(lag: Long) {
        consumerLag.set(lag)
    }
}
