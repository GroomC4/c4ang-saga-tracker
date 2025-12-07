package com.groom.saga.application.service

import com.groom.saga.application.dto.GetSagaQuery
import com.groom.saga.application.dto.SagaDetailResult
import com.groom.saga.application.dto.SagaStatisticsResult
import com.groom.saga.application.dto.SagaStepResult
import com.groom.saga.application.dto.SearchSagasQuery
import com.groom.saga.common.exception.SagaNotFoundException
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.port.LoadSagaPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class SagaQueryService(
    private val loadSagaPort: LoadSagaPort
) {

    fun getSaga(query: GetSagaQuery): SagaDetailResult {
        logger.debug { "Getting saga: sagaId=${query.sagaId}" }

        val instance = loadSagaPort.findBySagaId(query.sagaId)
            ?: throw SagaNotFoundException(query.sagaId)

        val steps = loadSagaPort.findStepsBySagaId(query.sagaId)

        return SagaDetailResult.from(instance, steps.size)
    }

    fun getSagaSteps(sagaId: String): List<SagaStepResult> {
        logger.debug { "Getting saga steps: sagaId=$sagaId" }

        // Saga 존재 여부 확인
        loadSagaPort.findBySagaId(sagaId)
            ?: throw SagaNotFoundException(sagaId)

        val steps = loadSagaPort.findStepsBySagaId(sagaId)

        return steps.map { SagaStepResult.from(it) }
            .sortedBy { it.recordedAt }
    }

    fun searchSagas(query: SearchSagasQuery): Page<SagaDetailResult> {
        logger.debug { "Searching sagas: $query" }

        val pageable = PageRequest.of(
            query.page,
            query.size.coerceIn(1, 100),
            Sort.by(Sort.Direction.DESC, "startedAt")
        )

        val page = loadSagaPort.searchSagas(
            orderId = query.orderId,
            sagaType = query.sagaType,
            status = query.status,
            fromDate = query.fromDate,
            toDate = query.toDate,
            pageable = pageable
        )

        return page.map { instance ->
            val stepCount = loadSagaPort.findStepsBySagaId(instance.sagaId).size
            SagaDetailResult.from(instance, stepCount)
        }
    }

    fun getStatistics(query: SearchSagasQuery): SagaStatisticsResult {
        logger.debug { "Getting statistics: $query" }

        val byStatus = loadSagaPort.countByStatus()
        val byType = loadSagaPort.countByType()

        val total = byStatus.values.sum()
        val failed = byStatus[SagaStatus.FAILED] ?: 0L
        val compensated = byStatus[SagaStatus.COMPENSATED] ?: 0L

        val failureRate = if (total > 0) failed.toDouble() / total else 0.0
        val compensationRate = if (failed > 0) compensated.toDouble() / failed else 0.0

        return SagaStatisticsResult(
            fromDate = query.fromDate,
            toDate = query.toDate,
            total = total,
            byStatus = byStatus,
            byType = byType,
            failureRate = failureRate,
            compensationRate = compensationRate
        )
    }
}
