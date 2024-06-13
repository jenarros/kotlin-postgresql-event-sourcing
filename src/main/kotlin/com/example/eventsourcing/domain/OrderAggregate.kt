package com.example.eventsourcing.domain

import com.example.eventsourcing.domain.model.OrderStatus
import com.example.eventsourcing.domain.model.OrderWaypoint
import com.example.eventsourcing.domain.model.event.Event
import com.fasterxml.jackson.annotation.JsonIgnore
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class OrderAggregate(
    override val aggregateId: UUID,
    override val version: Int,
    override val baseVersion: Int,
    @JsonIgnore
    override val changes: List<Event> = emptyList(),
    var status: OrderStatus? = null,
    var riderId: UUID? = null,
    var price: BigDecimal? = null,
    var route: List<OrderWaypoint> = emptyList(),
    var driverId: UUID? = null,
    var placedDate: Instant? = null,
    var acceptedDate: Instant? = null,
    var completedDate: Instant? = null,
    var cancelledDate: Instant? = null
) : Aggregate {

    override val aggregateType = AggregateType.ORDER
}

fun Aggregate.nextVersion() = version + 1
