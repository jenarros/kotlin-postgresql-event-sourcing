import com.example.eventsourcing.domain.OrderAggregate
import com.example.eventsourcing.domain.applyChange
import com.example.eventsourcing.domain.model.InvalidCommandError
import com.example.eventsourcing.domain.model.OrderStatus
import com.example.eventsourcing.domain.model.command.*
import com.example.eventsourcing.domain.model.event.*
import com.example.eventsourcing.domain.nextVersion
import java.util.*

fun OrderAggregate.process(command: PlaceOrderCommand): OrderAggregate {
    if (status != null) {
        throw InvalidCommandError("Can't place an order, it's already in status $status")
    }
    return applyChange(
        OrderPlacedEvent(
            aggregateId,
            nextVersion(),
            command.createdAt,
            command.riderId,
            command.price,
            command.route,
        )
    )
}

fun OrderAggregate.process(command: AdjustOrderPriceCommand): OrderAggregate {
    if (!EnumSet.of(OrderStatus.PLACED, OrderStatus.ADJUSTED).contains(status)) {
        throw InvalidCommandError("Can't adjust the price of an order in status $status")
    }
    return applyChange(
        OrderPriceAdjustedEvent(
            aggregateId,
            nextVersion(),
            command.createdAt,
            command.newPrice,
        )
    )
}

fun OrderAggregate.process(command: AcceptOrderCommand): OrderAggregate {
    if (EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.COMPLETED, OrderStatus.CANCELLED).contains(status)) {
        throw InvalidCommandError("Can't accept order in status $status")
    }
    return applyChange(
        OrderAcceptedEvent(
            aggregateId,
            nextVersion(),
            command.createdAt,
            command.driverId
        )
    )
}

fun OrderAggregate.process(command: CompleteOrderCommand): OrderAggregate {
    if (status != OrderStatus.ACCEPTED) {
        throw InvalidCommandError("Order in status $status can't be completed")
    }
    return applyChange(
        OrderCompletedEvent(
            aggregateId,
            nextVersion(),
            command.createdAt
        )
    )
}

fun OrderAggregate.process(command: CancelOrderCommand): OrderAggregate {
    if (!EnumSet.of(OrderStatus.PLACED, OrderStatus.ADJUSTED, OrderStatus.ACCEPTED).contains(status)) {
        throw InvalidCommandError("Order in status $status can't be cancelled")
    }
    return applyChange(
        OrderCancelledEvent(
            aggregateId,
            nextVersion(),
            command.createdAt
        )
    )
}