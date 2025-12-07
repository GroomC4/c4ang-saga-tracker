package com.groom.saga.application.dto

import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaType
import java.time.Instant

data class ProcessSagaEventCommand(
    val eventId: String,
    val eventTimestamp: Instant,
    val sagaId: String,
    val sagaType: SagaType,
    val step: String,
    val status: SagaStatus,
    val orderId: String,
    val metadata: Map<String, Any>?,
    val recordedAt: Instant
) {
    val traceId: String?
        get() = metadata?.get("traceId") as? String

    val producerService: String?
        get() = metadata?.get("producerService") as? String
}
