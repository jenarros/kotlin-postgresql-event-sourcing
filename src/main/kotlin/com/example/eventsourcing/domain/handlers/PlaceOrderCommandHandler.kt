package com.example.eventsourcing.domain.handlers

import com.example.eventsourcing.domain.model.Aggregate
import com.example.eventsourcing.domain.model.command.Command
import com.example.eventsourcing.domain.model.command.PlaceOrderCommand

class PlaceOrderCommandHandler : CommandHandler<PlaceOrderCommand> {
    override fun handle(aggregate: Aggregate, command: Command) {
        // Add additional business logic here.
        aggregate.process(command)
        // Also, add additional business logic here.
        // Read other aggregates using AggregateStore.
    }

    override val commandType = PlaceOrderCommand::class.java
}
