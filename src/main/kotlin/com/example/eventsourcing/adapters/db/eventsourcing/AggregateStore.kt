package com.example.eventsourcing.adapters.db.eventsourcing

import com.example.eventsourcing.adapters.db.eventsourcing.repository.AggregateRepository
import com.example.eventsourcing.adapters.db.eventsourcing.repository.EventRepository
import com.example.eventsourcing.config.EventSourcingProperties
import com.example.eventsourcing.config.SnapshottingProperties
import com.example.eventsourcing.domain.AggregateType
import com.example.eventsourcing.domain.model.event.Event
import com.example.eventsourcing.domain.model.event.EventWithId
import com.example.eventsourcing.domain.Aggregate
import com.example.eventsourcing.domain.OrderAggregate
import com.example.eventsourcing.domain.loadFromHistory
import org.slf4j.Logger
import java.util.*

class AggregateStore(
    private val aggregateRepository: AggregateRepository,
    private val eventRepository: EventRepository,
    private val properties: EventSourcingProperties,
    private val logger: Logger
) {
    fun saveAggregate(aggregate: Aggregate): List<EventWithId<Event>> {
        aggregateRepository.createAggregateIfAbsent(aggregate.aggregateType, aggregate.aggregateId)

        if (!aggregateRepository.checkAndUpdateAggregateVersion(
                aggregate.aggregateId,
                aggregate.baseVersion,
                aggregate.version
            )
        ) {
            logger.warn(
                "Optimistic concurrency control error in aggregate {} {}: " +
                        "actual version {} doesn't match expected version {}",
                aggregate.aggregateType, aggregate.aggregateId, aggregate.version, aggregate.baseVersion
            )
            throw OptimisticConcurrencyControlError(aggregate.baseVersion.toLong())
        }

        return aggregate.changes.map {
            logger.info("Appending {} event: {}", aggregate.aggregateType, it)
            eventRepository.appendEvent<Event>(it).also {
                createAggregateSnapshot(properties.getSnapshotting(aggregate.aggregateType), aggregate)
            }
        }
    }

    private fun createAggregateSnapshot(
        snapshotting: SnapshottingProperties,
        aggregate: Aggregate
    ) {
        if (snapshotting.enabled && snapshotting.nthEvent > 1 && aggregate.version % snapshotting.nthEvent == 0) {
            logger.info(
                "Creating {} aggregate {} version {} snapshot",
                aggregate.aggregateType, aggregate.aggregateId, aggregate.version
            )
            aggregateRepository.createAggregateSnapshot(aggregate)
        }
    }

    fun readAggregate(
        aggregateType: AggregateType,
        aggregateId: UUID,
        version: Int? = null
    ): Aggregate =
        if (properties.getSnapshotting(aggregateType).enabled) {
            readAggregateFromSnapshot(aggregateId, version)
                ?: readAggregateFromEvents(aggregateType, aggregateId, version)
                    .also {
                        logger.info("Aggregate {} snapshot not found", aggregateId)
                    }
        } else {
            readAggregateFromEvents(aggregateType, aggregateId, version)
        }

    private fun readAggregateFromSnapshot(
        aggregateId: UUID,
        aggregateVersion: Int? = null
    ): Aggregate? {
        return aggregateRepository.readAggregateSnapshot(aggregateId, aggregateVersion)
            ?.let { aggregate: Aggregate ->
                logger.debug("Read aggregate {} snapshot version {}", aggregateId, aggregate.version)

                if (aggregateVersion == null || aggregate.version < aggregateVersion) {
                    val events = eventRepository.readEvents(aggregateId, aggregate.version, aggregateVersion)
                        .map { it.event }
                        .toList()
                    logger.debug(
                        "Read {} events after version {} for aggregate {}",
                        events.size, aggregate.version, aggregateId
                    )
                    aggregate.loadFromHistory(events)
                } else {
                    aggregate
                }
            }
    }

    private fun readAggregateFromEvents(
        aggregateType: AggregateType,
        aggregateId: UUID,
        aggregateVersion: Int? = null
    ): Aggregate {
        val events = eventRepository.readEvents(aggregateId, aggregateVersion, null)
            .map { it.event }

        logger.debug("Read {} events for aggregate {}", events.size, aggregateId)
        return when (aggregateType) {
            AggregateType.ORDER -> OrderAggregate(
                aggregateId = aggregateId,
                version = aggregateVersion ?: 0,
                baseVersion = aggregateVersion ?: 0
            ).loadFromHistory(events)
        }
    }
}
