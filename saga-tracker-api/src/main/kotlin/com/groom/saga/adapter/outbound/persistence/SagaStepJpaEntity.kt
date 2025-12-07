package com.groom.saga.adapter.outbound.persistence

import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaStep
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.Instant

@Entity
@Table(name = "saga_steps")
class SagaStepJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "saga_id", length = 100, nullable = false)
    val sagaId: String,

    @Column(name = "event_id", length = 100, nullable = false, unique = true)
    val eventId: String,

    @Column(name = "step", length = 100, nullable = false)
    val step: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    val status: SagaStatus,

    @Column(name = "producer_service", length = 50)
    val producerService: String? = null,

    @Column(name = "trace_id", length = 100)
    val traceId: String? = null,

    @Type(JsonType::class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    val metadata: Map<String, Any>? = null,

    @Column(name = "recorded_at", nullable = false)
    val recordedAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain(): SagaStep = SagaStep(
        id = id,
        sagaId = sagaId,
        eventId = eventId,
        step = step,
        status = status,
        producerService = producerService,
        traceId = traceId,
        metadata = metadata,
        recordedAt = recordedAt,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: SagaStep): SagaStepJpaEntity = SagaStepJpaEntity(
            id = domain.id,
            sagaId = domain.sagaId,
            eventId = domain.eventId,
            step = domain.step,
            status = domain.status,
            producerService = domain.producerService,
            traceId = domain.traceId,
            metadata = domain.metadata,
            recordedAt = domain.recordedAt,
            createdAt = domain.createdAt
        )
    }
}
