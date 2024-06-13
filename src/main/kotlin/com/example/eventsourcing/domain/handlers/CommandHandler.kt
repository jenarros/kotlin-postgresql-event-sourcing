package com.example.eventsourcing.domain.handlers

import com.example.eventsourcing.domain.model.Aggregate
import com.example.eventsourcing.domain.model.command.Command

interface CommandHandler<T : Command> {
    fun handle(aggregate: Aggregate, command: Command)
    val commandType: Class<T>
}
