package com.groom.saga.adapter.outbound.persistence

import com.groom.saga.domain.model.SagaInstance
import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaStep
import com.groom.saga.domain.model.SagaType
import com.groom.saga.domain.port.LoadSagaPort
import com.groom.saga.domain.port.SaveSagaPort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class SagaPersistenceAdapter(
    private val sagaInstanceRepository: SagaInstanceJpaRepository,
    private val sagaStepRepository: SagaStepJpaRepository
) : LoadSagaPort, SaveSagaPort {

    // ===== LoadSagaPort =====

    @Transactional(readOnly = true)
    override fun findBySagaId(sagaId: String): SagaInstance? {
        return sagaInstanceRepository.findByIdOrNull(sagaId)?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findByOrderId(orderId: String): List<SagaInstance> {
        return sagaInstanceRepository.findByOrderId(orderId).map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun findStepsByEventId(eventId: String): SagaStep? {
        return sagaStepRepository.findByEventId(eventId)?.toDomain()
    }

    @Transactional(readOnly = true)
    override fun findStepsBySagaId(sagaId: String): List<SagaStep> {
        return sagaStepRepository.findBySagaIdOrderByRecordedAtAsc(sagaId).map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun searchSagas(
        orderId: String?,
        sagaType: SagaType?,
        status: SagaStatus?,
        fromDate: Instant?,
        toDate: Instant?,
        pageable: Pageable
    ): Page<SagaInstance> {
        val spec = SagaInstanceSpecifications.buildSearchSpec(
            orderId = orderId,
            sagaType = sagaType,
            status = status,
            fromDate = fromDate,
            toDate = toDate
        )
        return sagaInstanceRepository.findAll(spec, pageable).map { it.toDomain() }
    }

    @Transactional(readOnly = true)
    override fun countByStatus(): Map<SagaStatus, Long> {
        return sagaInstanceRepository.countByStatus()
            .associate { (it[0] as SagaStatus) to (it[1] as Long) }
    }

    @Transactional(readOnly = true)
    override fun countByType(): Map<SagaType, Long> {
        return sagaInstanceRepository.countByType()
            .associate { (it[0] as SagaType) to (it[1] as Long) }
    }

    // ===== SaveSagaPort =====

    @Transactional
    override fun save(sagaInstance: SagaInstance): SagaInstance {
        val existingEntity = sagaInstanceRepository.findByIdOrNull(sagaInstance.sagaId)

        val entity = if (existingEntity != null) {
            existingEntity.updateFrom(sagaInstance)
            existingEntity
        } else {
            SagaInstanceJpaEntity.fromDomain(sagaInstance)
        }

        return sagaInstanceRepository.save(entity).toDomain()
    }

    @Transactional
    override fun saveStep(sagaStep: SagaStep): SagaStep {
        val entity = SagaStepJpaEntity.fromDomain(sagaStep)
        return sagaStepRepository.save(entity).toDomain()
    }

    @Transactional
    override fun saveInstanceAndStep(sagaInstance: SagaInstance, sagaStep: SagaStep): Pair<SagaInstance, SagaStep> {
        val savedInstance = save(sagaInstance)
        val savedStep = saveStep(sagaStep)
        return Pair(savedInstance, savedStep)
    }
}
