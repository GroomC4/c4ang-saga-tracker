package com.groom.saga.configuration.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kafka.consumer")
data class KafkaConsumerProperties(
    val groupId: String = "saga-tracker-service",
    val autoOffsetReset: String = "earliest",
    val enableAutoCommit: Boolean = false,
    val maxPollRecords: Int = 500
)
