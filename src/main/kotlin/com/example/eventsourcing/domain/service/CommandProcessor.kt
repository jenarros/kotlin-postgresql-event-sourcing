package com.example.eventsourcing.domain.service

import com.example.eventsourcing.domain.model.Aggregate
import com.example.eventsourcing.domain.model.command.Command
import com.example.eventsourcing.domain.service.command.CommandHandler
import com.example.eventsourcing.domain.service.command.DefaultCommandHandler
import com.example.eventsourcing.domain.service.event.SyncEventHandler
import org.slf4j.LoggerFactory

class CommandProcessor(
    private val aggregateStore: AggregateStore,
    private val commandHandlers: List<CommandHandler<out Command>>,
    private val defaultCommandHandler: DefaultCommandHandler,
    private val aggregateChangesHandlers: List<SyncEventHandler>
) {
    fun process(command: Command): Aggregate {
        log.debug("Processing command {}", command)
        val aggregateType = command.aggregateType
        val aggregateId = command.aggregateId
        val aggregate = aggregateStore.readAggregate(aggregateType, aggregateId)
        commandHandlers
            .firstOrNull { it.commandType == command.javaClass }
            ?.let {
                it.handle(aggregate, command).also {
                    log.debug("Handling command {} with {}", command.javaClass.simpleName, it.javaClass.simpleName)
                }
            }
            ?: defaultCommandHandler.handle(aggregate, command)
                .also {
                    log.debug(
                        "No specialized handler found, handling command {} with {}",
                        command.javaClass.simpleName, defaultCommandHandler.javaClass.simpleName
                    )
                }
        aggregateChangesHandlers
            .filter { it.aggregateType === aggregateType }
            .forEach { it.handleEvents(aggregateStore.saveAggregate(aggregate), aggregate) }
        return aggregate
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommandProcessor::class.java)
    }
}
