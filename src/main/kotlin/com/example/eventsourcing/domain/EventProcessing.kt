package com.example.eventsourcing.domain

import com.example.eventsourcing.domain.model.OrderStatus
import com.example.eventsourcing.domain.model.command.*
import com.example.eventsourcing.domain.model.event.*
import process
import java.lang.reflect.InvocationTargetException

fun OrderAggregate.apply(event: OrderPlacedEvent): OrderAggregate {
    status = OrderStatus.PLACED
    riderId = event.riderId
    price = event.price
    route = event.route
    placedDate = event.createdAt
    return this
}

fun OrderAggregate.apply(event: OrderPriceAdjustedEvent): OrderAggregate {
    status = OrderStatus.ADJUSTED
    price = event.newPrice
    return this
}

fun OrderAggregate.apply(event: OrderAcceptedEvent): OrderAggregate {
    status = OrderStatus.ACCEPTED
    driverId = event.driverId
    acceptedDate = event.createdAt
    return this
}

fun OrderAggregate.apply(event: OrderCompletedEvent): OrderAggregate {
    status = OrderStatus.COMPLETED
    completedDate = event.createdAt
    return this
}

fun OrderAggregate.apply(event: OrderCancelledEvent): OrderAggregate {
    status = OrderStatus.CANCELLED
    cancelledDate = event.createdAt
    return this
}

fun OrderAggregate.applyChange(event: Event): OrderAggregate {
    check(event.version == nextVersion()) {
        "Event version ${event.version} doesn't match expected version ${nextVersion()}"
    }
    return when (event) {
        is OrderAcceptedEvent -> this.apply(event)
        is OrderCancelledEvent -> this.apply(event)
        is OrderCompletedEvent -> this.apply(event)
        is OrderPlacedEvent -> this.apply(event)
        is OrderPriceAdjustedEvent -> this.apply(event)
    }.copy(
        changes = changes.plus(event),
        version = event.version
    )
}

fun OrderAggregate.process(command: Command): OrderAggregate =
    when(command){
        is AcceptOrderCommand -> this.process(command)
        is AdjustOrderPriceCommand -> this.process(command)
        is CancelOrderCommand -> this.process(command)
        is CompleteOrderCommand -> this.process(command)
        is PlaceOrderCommand -> this.process(command)
    }

fun Aggregate.loadFromHistory(events: List<Event>): Aggregate {
    if(changes.isNotEmpty()) throw RuntimeException("Aggregate has non-empty changes")

    return events.fold(this) { acc: Aggregate, event: Event ->
        if(event.version <= version) {
            throw RuntimeException("Event version ${event.version} <= aggregate base version ${acc.baseVersion}")
        }
        when (acc) {
            is OrderAggregate -> when (event) {
                is OrderAcceptedEvent -> acc.apply(event)
                is OrderCancelledEvent -> acc.apply(event)
                is OrderCompletedEvent -> acc.apply(event)
                is OrderPlacedEvent -> acc.apply(event)
                is OrderPriceAdjustedEvent -> acc.apply(event)
            }.copy(version = event.version, baseVersion = event.version)

            else -> TODO("aggregate $acc not supported")
        }
    }
}