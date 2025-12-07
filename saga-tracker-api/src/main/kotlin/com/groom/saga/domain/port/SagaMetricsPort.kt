package com.groom.saga.domain.port

import com.groom.saga.domain.model.SagaStatus
import java.time.Duration

interface SagaMetricsPort {
    fun incrementProcessedEvents(step: String, status: SagaStatus)

    fun incrementFailed()

    fun incrementCompensated()

    fun recordCompensationDuration(duration: Duration)

    fun updateActiveSagas(status: SagaStatus, count: Long)

    fun updateConsumerLag(lag: Long)
}
