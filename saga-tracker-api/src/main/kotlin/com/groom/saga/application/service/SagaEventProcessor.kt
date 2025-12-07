package com.groom.saga.application.service

import com.groom.saga.application.dto.ProcessSagaEventCommand
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.port.LoadSagaPort
import com.groom.saga.domain.port.SagaMetricsPort
import com.groom.saga.domain.port.SaveSagaPort
import com.groom.saga.domain.service.SagaAggregator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Service
class SagaEventProcessor(
    private val loadSagaPort: LoadSagaPort,
    private val saveSagaPort: SaveSagaPort,
    private val sagaMetricsPort: SagaMetricsPort,
    private val sagaAggregator: SagaAggregator
) {

    @Transactional
    fun process(command: ProcessSagaEventCommand) {
        logger.debug { "Processing saga event: eventId=${command.eventId}, sagaId=${command.sagaId}, step=${command.step}" }

        // 1. 중복 이벤트 체크
        if (isDuplicateEvent(command.eventId)) {
            logger.info { "Duplicate event ignored: eventId=${command.eventId}" }
            return
        }

        // 2. 기존 Saga 인스턴스 조회
        val existingInstance = loadSagaPort.findBySagaId(command.sagaId)

        // 3. Step 생성
        val step = sagaAggregator.createStep(
            sagaId = command.sagaId,
            eventId = command.eventId,
            step = command.step,
            status = command.status,
            producerService = command.producerService,
            traceId = command.traceId,
            metadata = command.metadata,
            recordedAt = command.recordedAt
        )

        // 4. Instance 생성 또는 업데이트
        val updatedInstance = sagaAggregator.createOrUpdateInstance(
            existingInstance = existingInstance,
            sagaId = command.sagaId,
            sagaType = command.sagaType,
            orderId = command.orderId,
            step = step
        )

        // 5. 저장
        saveSagaPort.saveInstanceAndStep(updatedInstance, step)

        // 6. 메트릭 업데이트
        updateMetrics(command, existingInstance)

        logger.info {
            "Saga event processed: sagaId=${command.sagaId}, step=${command.step}, status=${command.status}"
        }
    }

    private fun isDuplicateEvent(eventId: String): Boolean {
        return loadSagaPort.findStepsByEventId(eventId) != null
    }

    private fun updateMetrics(command: ProcessSagaEventCommand, existingInstance: com.groom.saga.domain.model.SagaInstance?) {
        sagaMetricsPort.incrementProcessedEvents(command.step, command.status)

        when (command.status) {
            SagaStatus.FAILED -> sagaMetricsPort.incrementFailed()
            SagaStatus.COMPENSATED -> {
                sagaMetricsPort.incrementCompensated()
                // 보상 소요 시간 계산 (실패 시점 ~ 보상 완료 시점)
                existingInstance?.let { instance ->
                    if (instance.currentStatus == SagaStatus.FAILED) {
                        val duration = Duration.between(instance.updatedAt, command.recordedAt)
                        sagaMetricsPort.recordCompensationDuration(duration)
                    }
                }
            }
            else -> { /* no-op */ }
        }
    }
}
