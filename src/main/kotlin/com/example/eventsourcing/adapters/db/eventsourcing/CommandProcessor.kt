package com.example.eventsourcing.adapters.db.eventsourcing

import com.example.eventsourcing.domain.model.Aggregate
import com.example.eventsourcing.domain.model.command.Command
import com.example.eventsourcing.domain.handlers.CommandHandler
import com.example.eventsourcing.domain.handlers.DefaultCommandHandler
import com.example.eventsourcing.adapters.db.eventsourcing.handlers.SyncEventHandler
import org.slf4j.Logger

class CommandProcessor(
    private val aggregateStore: AggregateStore,
    private val commandHandlers: List<CommandHandler<out Command>>,
    private val defaultCommandHandler: DefaultCommandHandler,
    private val aggregateChangesHandlers: List<SyncEventHandler>,
    private val logger: Logger
) {
    fun process(command: Command): Aggregate {
        logger.debug("Processing command {}", command)
        val aggregateType = command.aggregateType
        val aggregateId = command.aggregateId
        val aggregate = aggregateStore.readAggregate(aggregateType, aggregateId)
        commandHandlers
            .firstOrNull { it.commandType == command.javaClass }
            ?.let {
                it.handle(aggregate, command).also {
                    logger.debug("Handling command {} with {}", command.javaClass.simpleName, it.javaClass.simpleName)
                }
            }
            ?: defaultCommandHandler.handle(aggregate, command)
                .also {
                    logger.debug(
                        "No specialized handler found, handling command {} with {}",
                        command.javaClass.simpleName, defaultCommandHandler.javaClass.simpleName
                    )
                }
        aggregateChangesHandlers
            .filter { it.aggregateType === aggregateType }
            .forEach { it.handleEvents(aggregateStore.saveAggregate(aggregate), aggregate) }
        return aggregate
    }

}
