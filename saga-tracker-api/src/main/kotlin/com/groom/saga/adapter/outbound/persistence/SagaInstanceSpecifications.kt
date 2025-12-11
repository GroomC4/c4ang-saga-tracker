package com.groom.saga.adapter.outbound.persistence

import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaType
import org.springframework.data.jpa.domain.Specification
import java.time.Instant

object SagaInstanceSpecifications {

    fun withOrderId(orderId: String?): Specification<SagaInstanceJpaEntity> =
        Specification { root, _, criteriaBuilder ->
            orderId?.let {
                criteriaBuilder.equal(root.get<String>("orderId"), it)
            }
        }

    fun withSagaType(sagaType: SagaType?): Specification<SagaInstanceJpaEntity> =
        Specification { root, _, criteriaBuilder ->
            sagaType?.let {
                criteriaBuilder.equal(root.get<SagaType>("sagaType"), it)
            }
        }

    fun withStatus(status: SagaStatus?): Specification<SagaInstanceJpaEntity> =
        Specification { root, _, criteriaBuilder ->
            status?.let {
                criteriaBuilder.equal(root.get<SagaStatus>("currentStatus"), it)
            }
        }

    fun withStartedAtFrom(fromDate: Instant?): Specification<SagaInstanceJpaEntity> =
        Specification { root, _, criteriaBuilder ->
            fromDate?.let {
                criteriaBuilder.greaterThanOrEqualTo(root.get("startedAt"), it)
            }
        }

    fun withStartedAtTo(toDate: Instant?): Specification<SagaInstanceJpaEntity> =
        Specification { root, _, criteriaBuilder ->
            toDate?.let {
                criteriaBuilder.lessThanOrEqualTo(root.get("startedAt"), it)
            }
        }

    fun buildSearchSpec(
        orderId: String?,
        sagaType: SagaType?,
        status: SagaStatus?,
        fromDate: Instant?,
        toDate: Instant?
    ): Specification<SagaInstanceJpaEntity> {
        return Specification.where(withOrderId(orderId))
            .and(withSagaType(sagaType))
            .and(withStatus(status))
            .and(withStartedAtFrom(fromDate))
            .and(withStartedAtTo(toDate))
    }
}
