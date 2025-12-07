package com.groom.saga.configuration.kafka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kafka.topics")
data class KafkaTopicProperties(
    val sagaTracker: String = "c4ang.saga.tracker",
    val sagaTrackerDlt: String = "c4ang.saga.tracker.dlt"
)
