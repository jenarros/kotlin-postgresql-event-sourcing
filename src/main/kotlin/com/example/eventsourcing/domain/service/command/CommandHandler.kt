package com.example.eventsourcing.domain.service.command

import com.example.eventsourcing.domain.model.Aggregate
import com.example.eventsourcing.domain.model.command.Command

interface CommandHandler<T : Command> {
    fun handle(aggregate: com.example.eventsourcing.domain.model.Aggregate, command: Command)
    val commandType: Class<T>
}
