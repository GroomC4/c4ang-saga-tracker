package com.groom.saga.common.exception

class SagaNotFoundException(sagaId: String) : RuntimeException("Saga not found: $sagaId")
