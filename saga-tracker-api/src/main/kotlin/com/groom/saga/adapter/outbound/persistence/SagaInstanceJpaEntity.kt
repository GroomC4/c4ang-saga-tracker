package com.groom.saga.adapter.outbound.persistence

import com.groom.saga.domain.model.SagaInstance
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "saga_instance")
class SagaInstanceJpaEntity(
    @Id
    @Column(name = "saga_id", length = 100)
    val sagaId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_type", length = 50, nullable = false)
    val sagaType: SagaType,

    @Column(name = "order_id", length = 100, nullable = false)
    val orderId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", length = 20, nullable = false)
    var currentStatus: SagaStatus,

    @Column(name = "last_step", length = 100)
    var lastStep: String? = null,

    @Column(name = "last_trace_id", length = 100)
    var lastTraceId: String? = null,

    @Column(name = "started_at", nullable = false)
    val startedAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain(): SagaInstance = SagaInstance(
        sagaId = sagaId,
        sagaType = sagaType,
        orderId = orderId,
        currentStatus = currentStatus,
        lastStep = lastStep,
        lastTraceId = lastTraceId,
        startedAt = startedAt,
        updatedAt = updatedAt,
        createdAt = createdAt
    )

    fun updateFrom(domain: SagaInstance) {
        currentStatus = domain.currentStatus
        lastStep = domain.lastStep
        lastTraceId = domain.lastTraceId
        updatedAt = domain.updatedAt
    }

    companion object {
        fun fromDomain(domain: SagaInstance): SagaInstanceJpaEntity = SagaInstanceJpaEntity(
            sagaId = domain.sagaId,
            sagaType = domain.sagaType,
            orderId = domain.orderId,
            currentStatus = domain.currentStatus,
            lastStep = domain.lastStep,
            lastTraceId = domain.lastTraceId,
            startedAt = domain.startedAt,
            updatedAt = domain.updatedAt,
            createdAt = domain.createdAt
        )
    }
}
