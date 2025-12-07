package com.groom.saga.application.dto

import com.groom.saga.domain.model.SagaInstance
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaStep
import com.groom.saga.domain.model.SagaType
import java.time.Instant

data class SagaDetailResult(
    val sagaId: String,
    val sagaType: SagaType,
    val orderId: String,
    val currentStatus: SagaStatus,
    val lastStep: String?,
    val lastTraceId: String?,
    val startedAt: Instant,
    val updatedAt: Instant,
    val stepCount: Int
) {
    companion object {
        fun from(instance: SagaInstance, stepCount: Int): SagaDetailResult = SagaDetailResult(
            sagaId = instance.sagaId,
            sagaType = instance.sagaType,
            orderId = instance.orderId,
            currentStatus = instance.currentStatus,
            lastStep = instance.lastStep,
            lastTraceId = instance.lastTraceId,
            startedAt = instance.startedAt,
            updatedAt = instance.updatedAt,
            stepCount = stepCount
        )
    }
}

data class SagaStepResult(
    val id: Long?,
    val eventId: String,
    val step: String,
    val status: SagaStatus,
    val producerService: String?,
    val traceId: String?,
    val metadata: Map<String, Any>?,
    val recordedAt: Instant
) {
    companion object {
        fun from(step: SagaStep): SagaStepResult = SagaStepResult(
            id = step.id,
            eventId = step.eventId,
            step = step.step,
            status = step.status,
            producerService = step.producerService,
            traceId = step.traceId,
            metadata = step.metadata,
            recordedAt = step.recordedAt
        )
    }
}

data class SagaStatisticsResult(
    val fromDate: Instant?,
    val toDate: Instant?,
    val total: Long,
    val byStatus: Map<SagaStatus, Long>,
    val byType: Map<SagaType, Long>,
    val failureRate: Double,
    val compensationRate: Double
)
