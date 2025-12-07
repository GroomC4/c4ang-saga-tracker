package com.groom.saga.application.dto

import com.groom.saga.domain.model.SagaStatus
import com.groom.saga.domain.model.SagaType
import java.time.Instant

data class SearchSagasQuery(
    val orderId: String? = null,
    val sagaType: SagaType? = null,
    val status: SagaStatus? = null,
    val fromDate: Instant? = null,
    val toDate: Instant? = null,
    val page: Int = 0,
    val size: Int = 20
)
