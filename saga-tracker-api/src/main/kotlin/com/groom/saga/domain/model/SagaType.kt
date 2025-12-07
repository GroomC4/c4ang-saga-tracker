package com.groom.saga.domain.model

enum class SagaType {
    ORDER_CREATION,
    PAYMENT_COMPLETION;

    companion object {
        fun fromString(value: String): SagaType =
            entries.find { it.name == value }
                ?: throw IllegalArgumentException("Unknown SagaType: $value")
    }
}
