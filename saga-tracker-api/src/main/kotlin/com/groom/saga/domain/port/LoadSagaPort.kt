package com.groom.saga.domain.port

import com.groom.saga.domain.model.SagaInstance
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaStep
import com.groom.saga.domain.model.SagaType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.Instant

interface LoadSagaPort {
    fun findBySagaId(sagaId: String): SagaInstance?

    fun findByOrderId(orderId: String): List<SagaInstance>

    fun findStepsByEventId(eventId: String): SagaStep?

    fun findStepsBySagaId(sagaId: String): List<SagaStep>

    fun searchSagas(
        orderId: String? = null,
        sagaType: SagaType? = null,
        status: SagaStatus? = null,
        fromDate: Instant? = null,
        toDate: Instant? = null,
        pageable: Pageable
    ): Page<SagaInstance>

    fun countByStatus(): Map<SagaStatus, Long>

    fun countByType(): Map<SagaType, Long>
}
