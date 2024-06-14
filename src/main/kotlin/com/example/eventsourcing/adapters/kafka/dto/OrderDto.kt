package com.example.eventsourcing.adapters.kafka.dto

import com.example.eventsourcing.domain.model.OrderStatus
import com.example.eventsourcing.domain.model.OrderWaypoint
import java.math.BigDecimal
import java.util.*

data class OrderDto(
    val orderId: UUID,

    val eventType: String,

    val eventTimestamp: Long,

    val version: Int,

    val status: OrderStatus?,

    val riderId: UUID?,

    val price: BigDecimal?,

    val route: List<OrderWaypoint>?,

    val driverId: UUID?
)
