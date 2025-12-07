package com.groom.saga.adapter.outbound.persistence

import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SagaInstanceJpaRepository : JpaRepository<SagaInstanceJpaEntity, String> {

    fun findByOrderId(orderId: String): List<SagaInstanceJpaEntity>

    @Query(
        """
        SELECT s FROM SagaInstanceJpaEntity s
        WHERE (:orderId IS NULL OR s.orderId = :orderId)
          AND (:sagaType IS NULL OR s.sagaType = :sagaType)
          AND (:status IS NULL OR s.currentStatus = :status)
          AND (:fromDate IS NULL OR s.startedAt >= :fromDate)
          AND (:toDate IS NULL OR s.startedAt <= :toDate)
        """
    )
    fun search(
        @Param("orderId") orderId: String?,
        @Param("sagaType") sagaType: SagaType?,
        @Param("status") status: SagaStatus?,
        @Param("fromDate") fromDate: Instant?,
        @Param("toDate") toDate: Instant?,
        pageable: Pageable
    ): Page<SagaInstanceJpaEntity>

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
