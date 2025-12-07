package com.groom.saga.domain.service

import com.groom.saga.domain.model.SagaInstance
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaStep
import com.groom.saga.domain.model.SagaType
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SagaAggregator {

    fun createOrUpdateInstance(
        existingInstance: SagaInstance?,
        sagaId: String,
        sagaType: SagaType,
        orderId: String,
        step: SagaStep
    ): SagaInstance {
        return if (existingInstance == null) {
            createNewInstance(sagaId, sagaType, orderId, step)
        } else {
            existingInstance.updateWithStep(step)
        }
    }

    private fun createNewInstance(
        sagaId: String,
        sagaType: SagaType,
        orderId: String,
        step: SagaStep
    ): SagaInstance {
        return SagaInstance.create(
            sagaId = sagaId,
            sagaType = sagaType,
            orderId = orderId,
            initialStep = step.step,
            traceId = step.traceId
        ).copy(currentStatus = step.status)
    }

    fun createStep(
        sagaId: String,
        eventId: String,
        step: String,
        status: SagaStatus,
        producerService: String?,
        traceId: String?,
        metadata: Map<String, Any>?,
        recordedAt: Instant
    ): SagaStep {
        return SagaStep.create(
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
