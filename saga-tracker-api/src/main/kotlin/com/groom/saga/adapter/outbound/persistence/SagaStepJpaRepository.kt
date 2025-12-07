package com.groom.saga.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SagaStepJpaRepository : JpaRepository<SagaStepJpaEntity, Long> {

    fun findByEventId(eventId: String): SagaStepJpaEntity?

    fun findBySagaIdOrderByRecordedAtAsc(sagaId: String): List<SagaStepJpaEntity>

    fun countBySagaId(sagaId: String): Long
}
