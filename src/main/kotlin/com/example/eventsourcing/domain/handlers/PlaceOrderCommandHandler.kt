package com.example.eventsourcing.domain.handlers

import com.example.eventsourcing.domain.Aggregate
import com.example.eventsourcing.domain.OrderAggregate
import com.example.eventsourcing.domain.model.command.Command
import com.example.eventsourcing.domain.model.command.PlaceOrderCommand
import com.example.eventsourcing.domain.process

class PlaceOrderCommandHandler : CommandHandler<PlaceOrderCommand> {
    override fun handle(aggregate: Aggregate, command: Command): Aggregate {
        // Add additional business logic here.
        return (aggregate as OrderAggregate).process(command)
        // Also, add additional business logic here.
        // Read other aggregates using AggregateStore.
    }

    override val commandType = PlaceOrderCommand::class.java
}
