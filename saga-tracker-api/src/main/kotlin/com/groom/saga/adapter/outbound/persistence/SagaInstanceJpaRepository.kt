package com.groom.saga.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface SagaInstanceJpaRepository :
    JpaRepository<SagaInstanceJpaEntity, String>,
    JpaSpecificationExecutor<SagaInstanceJpaEntity> {

    fun findByOrderId(orderId: String): List<SagaInstanceJpaEntity>

    @Query(
        """
        SELECT s.currentStatus, COUNT(s)
        FROM SagaInstanceJpaEntity s
        GROUP BY s.currentStatus
        """
    )
    fun countByStatus(): List<Array<Any>>

    @Query(
        """
        SELECT s.sagaType, COUNT(s)
        FROM SagaInstanceJpaEntity s
        GROUP BY s.sagaType
        """
    )
    fun countByType(): List<Array<Any>>
}
