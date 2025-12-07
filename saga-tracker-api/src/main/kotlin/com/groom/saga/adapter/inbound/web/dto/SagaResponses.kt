package com.groom.saga.adapter.inbound.web.dto

import com.groom.saga.application.dto.SagaDetailResult
import com.groom.saga.application.dto.SagaStatisticsResult
import com.groom.saga.application.dto.SagaStepResult
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaType
import java.time.Instant

data class SagaDetailResponse(
    val sagaId: String,
    val sagaType: String,
    val orderId: String,
    val currentStatus: String,
    val lastStep: String?,
    val lastTraceId: String?,
    val startedAt: Instant,
    val updatedAt: Instant,
    val stepCount: Int
) {
    companion object {
        fun from(result: SagaDetailResult) = SagaDetailResponse(
            sagaId = result.sagaId,
            sagaType = result.sagaType.name,
            orderId = result.orderId,
            currentStatus = result.currentStatus.name,
            lastStep = result.lastStep,
            lastTraceId = result.lastTraceId,
            startedAt = result.startedAt,
            updatedAt = result.updatedAt,
            stepCount = result.stepCount
        )
    }
}

data class SagaStepResponse(
    val id: Long?,
    val eventId: String,
    val step: String,
    val status: String,
    val producerService: String?,
    val traceId: String?,
    val metadata: Map<String, Any>?,
    val recordedAt: Instant
) {
    companion object {
        fun from(result: SagaStepResult) = SagaStepResponse(
            id = result.id,
            eventId = result.eventId,
            step = result.step,
            status = result.status.name,
            producerService = result.producerService,
            traceId = result.traceId,
            metadata = result.metadata,
            recordedAt = result.recordedAt
        )
    }
}

data class SagaStepsResponse(
    val sagaId: String,
    val steps: List<SagaStepResponse>
)

data class SagaListResponse(
    val content: List<SagaDetailResponse>,
    val page: PageInfo
)

data class PageInfo(
    val number: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class SagaStatisticsResponse(
    val period: PeriodInfo,
    val total: Long,
    val byStatus: Map<String, Long>,
    val byType: Map<String, Long>,
    val failureRate: Double,
    val compensationRate: Double
) {
    companion object {
        fun from(result: SagaStatisticsResult) = SagaStatisticsResponse(
            period = PeriodInfo(result.fromDate, result.toDate),
            total = result.total,
            byStatus = result.byStatus.mapKeys { it.key.name },
            byType = result.byType.mapKeys { it.key.name },
            failureRate = result.failureRate,
            compensationRate = result.compensationRate
        )
    }
}

data class PeriodInfo(
    val from: Instant?,
    val to: Instant?
)
