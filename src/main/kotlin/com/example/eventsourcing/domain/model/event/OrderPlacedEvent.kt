package com.example.eventsourcing.domain.model.event

import com.example.eventsourcing.domain.model.OrderWaypoint
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class OrderPlacedEvent(
    aggregateId: UUID,
    version: Int,
    createdAt: Instant,
    val riderId: UUID,
    val price: BigDecimal,
    val route: List<OrderWaypoint>
) : Event(aggregateId, version, createdAt)
