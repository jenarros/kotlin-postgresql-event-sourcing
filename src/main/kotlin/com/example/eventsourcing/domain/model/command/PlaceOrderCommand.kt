package com.example.eventsourcing.domain.model.command

import com.example.eventsourcing.domain.model.AggregateType
import com.example.eventsourcing.domain.model.OrderWaypoint
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class PlaceOrderCommand(
    val riderId: UUID,
    val price: BigDecimal,
    val route: List<OrderWaypoint>,
    override val createdAt: Instant
) : Command {
    override val aggregateType: AggregateType = AggregateType.ORDER

    override val aggregateId: UUID = UUID.randomUUID()
}
