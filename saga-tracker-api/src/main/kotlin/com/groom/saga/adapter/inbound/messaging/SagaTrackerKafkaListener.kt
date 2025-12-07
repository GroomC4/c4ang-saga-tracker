package com.groom.saga.adapter.inbound.messaging

import com.groom.ecommerce.saga.event.avro.SagaTracker
import com.groom.saga.application.dto.ProcessSagaEventCommand
import com.groom.saga.application.service.SagaEventProcessor
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Component
class SagaTrackerKafkaListener(
    private val sagaEventProcessor: SagaEventProcessor,
    private val objectMapper: ObjectMapper
) {

    @KafkaListener(
        topics = ["\${kafka.topics.saga-tracker}"],
        groupId = "\${kafka.consumer.group-id}",
        containerFactory = "sagaTrackerKafkaListenerContainerFactory"
    )
    fun onMessage(record: ConsumerRecord<String, SagaTracker>, ack: Acknowledgment) {
        val sagaTracker = record.value()

        logger.debug {
            "Received SagaTracker event: key=${record.key()}, " +
                "eventId=${sagaTracker.eventId}, sagaId=${sagaTracker.sagaId}"
        }

        try {
            val command = toCommand(sagaTracker)
            sagaEventProcessor.process(command)
            ack.acknowledge()

            logger.info {
                "Successfully processed SagaTracker event: eventId=${sagaTracker.eventId}, " +
                    "sagaId=${sagaTracker.sagaId}, step=${sagaTracker.step}"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to process SagaTracker event: eventId=${sagaTracker.eventId}, " +
                    "sagaId=${sagaTracker.sagaId}, error=${e.message}"
            }
            // DLQ로 전송되도록 예외 재발생
            throw e
        }
    }

    private fun toCommand(sagaTracker: SagaTracker): ProcessSagaEventCommand {
        val metadata: Map<String, Any>? = sagaTracker.metadata?.let {
            try {
                objectMapper.readValue<Map<String, Any>>(it)
            } catch (e: Exception) {
                logger.warn { "Failed to parse metadata: ${e.message}" }
                null
            }
        }

        return ProcessSagaEventCommand(
            eventId = sagaTracker.eventId,
            eventTimestamp = Instant.ofEpochMilli(sagaTracker.eventTimestamp),
            sagaId = sagaTracker.sagaId,
            sagaType = SagaType.valueOf(sagaTracker.sagaType.name),
            step = sagaTracker.step,
            status = SagaStatus.valueOf(sagaTracker.status.name),
            orderId = sagaTracker.orderId,
            metadata = metadata,
            recordedAt = Instant.ofEpochMilli(sagaTracker.recordedAt)
        )
    }
}
