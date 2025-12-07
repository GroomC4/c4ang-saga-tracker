package com.groom.saga.configuration.kafka

import com.groom.ecommerce.saga.event.avro.SagaTracker
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaConsumerProperties::class, KafkaTopicProperties::class)
class KafkaConsumerConfig(
    private val consumerProperties: KafkaConsumerProperties,
    @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String,
    @Value("\${kafka.schema-registry.url}") private val schemaRegistryUrl: String
) {

    @Bean
    fun sagaTrackerConsumerFactory(): ConsumerFactory<String, SagaTracker> {
        val props = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to consumerProperties.groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to consumerProperties.autoOffsetReset,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to consumerProperties.enableAutoCommit,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to consumerProperties.maxPollRecords,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl,
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true
        )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun sagaTrackerKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, SagaTracker> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, SagaTracker>()
        factory.consumerFactory = sagaTrackerConsumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE

        // Error handler with retry (3 attempts, 1 second interval)
        val errorHandler = DefaultErrorHandler(
            FixedBackOff(1000L, 3L)
        )
        factory.setCommonErrorHandler(errorHandler)

        return factory
    }
}
