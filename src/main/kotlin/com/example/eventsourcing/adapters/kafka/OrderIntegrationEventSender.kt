package com.example.eventsourcing.adapters.kafka

import com.example.eventsourcing.domain.model.AggregateType
import com.example.eventsourcing.domain.model.OrderAggregate
import com.example.eventsourcing.domain.model.event.Event
import com.example.eventsourcing.domain.model.event.EventWithId
import com.example.eventsourcing.adapters.kafka.dto.OrderDto
import com.example.eventsourcing.adapters.kafka.OrderMapper.toDto
import com.example.eventsourcing.adapters.db.eventsourcing.AggregateStore
import com.example.eventsourcing.adapters.db.eventsourcing.handlers.AsyncEventHandler
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger

class OrderIntegrationEventSender(
    private val aggregateStore: AggregateStore,
    private val kafkaTemplate: KafkaProducer<String, String>,
    private val kafkaTopic: String,
    private val objectMapper: ObjectMapper,
    private val log: Logger
) : AsyncEventHandler {
    override fun handleEvent(eventWithId: EventWithId<Event>) {
        val event = eventWithId.event
        val aggregate = aggregateStore.readAggregate(
            AggregateType.ORDER, event.aggregateId, event.version
        )
        val orderDto = toDto(event, aggregate as OrderAggregate)
        sendDataToKafka(orderDto, kafkaTopic)
    }

    private fun sendDataToKafka(orderDto: OrderDto, topic: String) {
        log.info("Publishing integration event {}", orderDto)
        kafkaTemplate.send(
            ProducerRecord(
                topic,
                orderDto.orderId.toString(),
                objectMapper.writeValueAsString(orderDto)
            )
        )
    }

    override val aggregateType: AggregateType = AggregateType.ORDER
}
