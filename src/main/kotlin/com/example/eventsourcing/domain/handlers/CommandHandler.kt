package com.example.eventsourcing.domain.handlers

import com.example.eventsourcing.domain.Aggregate
import com.example.eventsourcing.domain.model.command.Command

interface CommandHandler<T : Command> {
    fun handle(aggregate: Aggregate, command: Command): Aggregate
    val commandType: Class<T>
}
