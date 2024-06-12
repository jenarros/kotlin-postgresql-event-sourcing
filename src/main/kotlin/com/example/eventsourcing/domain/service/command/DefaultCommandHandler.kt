package com.example.eventsourcing.domain.service.command

import com.example.eventsourcing.domain.model.Aggregate
import com.example.eventsourcing.domain.model.command.Command

class DefaultCommandHandler : CommandHandler<Command> {
    override fun handle(aggregate: Aggregate, command: Command) {
        aggregate.process(command)
    }

    override val commandType = Command::class.java
}
