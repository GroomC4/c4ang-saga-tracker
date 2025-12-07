package com.groom.saga.domain.model

enum class SagaStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    COMPENSATED;

    companion object {
        fun fromString(value: String): SagaStatus =
            entries.find { it.name == value }
                ?: throw IllegalArgumentException("Unknown SagaStatus: $value")
    }

    fun isTerminal(): Boolean = this in setOf(COMPLETED, FAILED, COMPENSATED)
}
