package com.example.eventsourcing.adapters.db.eventsourcing

import com.example.eventsourcing.adapters.db.eventsourcing.handlers.AsyncEventHandler
import org.slf4j.Logger
import java.util.function.Consumer

class ScheduledEventSubscriptionProcessor(
    private val eventHandlers: List<AsyncEventHandler>,
    private val eventSubscriptionProcessor: EventSubscriptionProcessor,
    private val logger: Logger
) {
    fun processNewEvents() {
        eventHandlers.forEach(Consumer { eventHandler: AsyncEventHandler -> this.processNewEvents(eventHandler) })
    }

    private fun processNewEvents(eventHandler: AsyncEventHandler) {
        try {
            eventSubscriptionProcessor.processNewEvents(eventHandler)
        } catch (e: Exception) {
            logger.warn(
                "Failed to handle new events for subscription %s"
                    .format(eventHandler.subscriptionName()), e
            )
        }
    }
}
