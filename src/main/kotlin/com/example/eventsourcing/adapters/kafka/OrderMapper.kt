package com.example.eventsourcing.adapters.kafka

import com.example.eventsourcing.adapters.kafka.dto.OrderDto
import com.example.eventsourcing.domain.OrderAggregate
import com.example.eventsourcing.domain.model.event.Event

object OrderMapper {
    fun toDto(
        event: Event,
        order: OrderAggregate
    ): OrderDto = OrderDto(
        order.aggregateId,
        event.eventType.name,
        event.createdAt.toEpochMilli(),
        order.baseVersion,
        order.status,
        order.riderId,
        order.price,
        order.route,
        order.driverId
    )
}
