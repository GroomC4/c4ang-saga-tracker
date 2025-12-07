package com.groom.saga.domain.model

import java.time.Instant

data class SagaStep(
    val id: Long? = null,
    val sagaId: String,
    val eventId: String,
    val step: String,
    val status: SagaStatus,
    val producerService: String? = null,
    val traceId: String? = null,
    val metadata: Map<String, Any>? = null,
    val recordedAt: Instant,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun create(
            sagaId: String,
            eventId: String,
            step: String,
            status: SagaStatus,
            producerService: String? = null,
            traceId: String? = null,
            metadata: Map<String, Any>? = null,
            recordedAt: Instant
        ): SagaStep = SagaStep(
            sagaId = sagaId,
            eventId = eventId,
            step = step,
            status = status,
            producerService = producerService,
            traceId = traceId,
            metadata = metadata,
            recordedAt = recordedAt
        )
    }
}
