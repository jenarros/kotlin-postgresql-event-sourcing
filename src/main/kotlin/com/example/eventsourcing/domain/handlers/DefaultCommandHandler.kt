package com.example.eventsourcing.domain.handlers

import com.example.eventsourcing.domain.Aggregate
import com.example.eventsourcing.domain.OrderAggregate
import com.example.eventsourcing.domain.model.command.*
import com.example.eventsourcing.domain.process

class DefaultCommandHandler : CommandHandler<Command> {
    override fun handle(aggregate: Aggregate, command: Command): Aggregate =
        when (command) {
            is PlaceOrderCommand -> (aggregate as OrderAggregate).process(command)
            is AcceptOrderCommand -> (aggregate as OrderAggregate).process(command)
            is AdjustOrderPriceCommand -> (aggregate as OrderAggregate).process(command)
            is CancelOrderCommand -> (aggregate as OrderAggregate).process(command)
            is CompleteOrderCommand -> (aggregate as OrderAggregate).process(command)
        }

    override val commandType = Command::class.java
}
