package com.example.eventsourcing.config

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.CreateTopicsResult
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.IntegerDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

object Kafka {
    const val TOPIC_ORDER_EVENTS = "order-events"

    fun kafkaProducer(kafkaBootstrapServers: String, topic: String): KafkaProducer<String, String> {
        createTopics(topic)

        val kafkaProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 1
        )

        return KafkaProducer<String, String>(kafkaProps)
    }

    fun kafkaConsumer(kafkaBootstrapServers: String, topicsToConsume: List<String>): Consumer<String, String> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaBootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = this.javaClass.simpleName + "-consumer"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        props[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] = "10"
        props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = "30000"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = IntegerDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

        return KafkaConsumer(
            props,
            StringDeserializer(),
            StringDeserializer()
        ).also {
            it.subscribe(topicsToConsume)
        }
    }

    private fun createTopics(topic: String): CreateTopicsResult {
        val props = Properties().also {
            it[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        }

        val adminClient = AdminClient.create(props)

        return adminClient.createTopics(
            listOf(
                NewTopic(topic, 10, 1)
            )
        )
    }
}
