package com.groom.saga.domain.model

import java.time.Instant

data class SagaInstance(
    val sagaId: String,
    val sagaType: SagaType,
    val orderId: String,
    val currentStatus: SagaStatus,
    val lastStep: String? = null,
    val lastTraceId: String? = null,
    val startedAt: Instant,
    val updatedAt: Instant,
    val createdAt: Instant = Instant.now(),
    val steps: List<SagaStep> = emptyList()
) {
    companion object {
        fun create(
            sagaId: String,
            sagaType: SagaType,
            orderId: String,
            initialStep: String,
            traceId: String? = null
        ): SagaInstance {
            val now = Instant.now()
            return SagaInstance(
                sagaId = sagaId,
                sagaType = sagaType,
                orderId = orderId,
                currentStatus = SagaStatus.STARTED,
                lastStep = initialStep,
                lastTraceId = traceId,
                startedAt = now,
                updatedAt = now,
                createdAt = now
            )
        }
    }

    fun updateWithStep(step: SagaStep): SagaInstance {
        val newStatus = determineNewStatus(step.status)
        return copy(
            currentStatus = newStatus,
            lastStep = step.step,
            lastTraceId = step.traceId,
            updatedAt = Instant.now()
        )
    }

    private fun determineNewStatus(stepStatus: SagaStatus): SagaStatus {
        return when (stepStatus) {
            SagaStatus.STARTED -> if (currentStatus == SagaStatus.STARTED) SagaStatus.STARTED else currentStatus
            SagaStatus.IN_PROGRESS -> SagaStatus.IN_PROGRESS
            SagaStatus.COMPLETED -> SagaStatus.COMPLETED
            SagaStatus.FAILED -> SagaStatus.FAILED
            SagaStatus.COMPENSATED -> SagaStatus.COMPENSATED
        }
    }

    fun isTerminated(): Boolean = currentStatus.isTerminal()

    fun withSteps(steps: List<SagaStep>): SagaInstance = copy(steps = steps)
}
