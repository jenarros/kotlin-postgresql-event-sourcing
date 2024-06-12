package com.example.eventsourcing.domain.service

import com.example.eventsourcing.domain.service.event.AsyncEventHandler
import org.slf4j.LoggerFactory
import java.util.function.Consumer

class ScheduledEventSubscriptionProcessor(
    private val eventHandlers: List<AsyncEventHandler>,
    private val eventSubscriptionProcessor: EventSubscriptionProcessor
) {
    fun processNewEvents() {
        eventHandlers.forEach(Consumer { eventHandler: AsyncEventHandler -> this.processNewEvents(eventHandler) })
    }

    private fun processNewEvents(eventHandler: AsyncEventHandler) {
        try {
            eventSubscriptionProcessor.processNewEvents(eventHandler)
        } catch (e: Exception) {
            log.warn(
                "Failed to handle new events for subscription %s"
                    .format(eventHandler.subscriptionName()), e
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ScheduledEventSubscriptionProcessor::class.java)
    }
}
