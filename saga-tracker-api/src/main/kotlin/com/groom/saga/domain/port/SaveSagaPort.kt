package com.groom.saga.domain.port

import com.groom.saga.domain.model.SagaInstance
import com.groom.saga.domain.model.SagaStep

interface SaveSagaPort {
    fun save(sagaInstance: SagaInstance): SagaInstance

    fun saveStep(sagaStep: SagaStep): SagaStep

    fun saveInstanceAndStep(sagaInstance: SagaInstance, sagaStep: SagaStep): Pair<SagaInstance, SagaStep>
}
