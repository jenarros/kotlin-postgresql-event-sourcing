package com.example.eventsourcing.adapters.db.eventsourcing

import com.example.eventsourcing.domain.model.event.EventSubscriptionCheckpoint
import com.example.eventsourcing.adapters.db.eventsourcing.handlers.AsyncEventHandler
import com.example.eventsourcing.adapters.db.eventsourcing.repository.EventRepository
import com.example.eventsourcing.adapters.db.eventsourcing.repository.EventSubscriptionRepository
import org.slf4j.Logger

class EventSubscriptionProcessor(
    private val subscriptionRepository: EventSubscriptionRepository,
    private val eventRepository: EventRepository,
    private val logger: Logger
) {
    fun processNewEvents(eventHandler: AsyncEventHandler) {
        val subscriptionName = eventHandler.subscriptionName()
        logger.debug("Handling new events for subscription {}", subscriptionName)
        subscriptionRepository.createSubscriptionIfAbsent(subscriptionName)
        subscriptionRepository.readCheckpointAndLockSubscription(subscriptionName)
            ?.let { checkpoint: EventSubscriptionCheckpoint ->
                logger.debug("Acquired lock on subscription {}, checkpoint = {}", subscriptionName, checkpoint)
                val events = eventRepository.readEventsAfterCheckpoint(
                    eventHandler.aggregateType,
                    checkpoint.lastProcessedTransactionId,
                    checkpoint.lastProcessedEventId
                )
                logger.debug("Fetched {} new event(s) for subscription {}", events.size, subscriptionName)
                if (events.isNotEmpty()) {
                    events.forEach { eventHandler.handleEvent(it) }
                    val lastEvent = events[events.size - 1]
                    subscriptionRepository.updateEventSubscription(
                        subscriptionName, lastEvent.transactionId, lastEvent.id
                    )
                }
            } ?: logger.debug("Can't acquire lock on subscription {}", subscriptionName)
    }
}
