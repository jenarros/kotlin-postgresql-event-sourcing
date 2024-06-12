package com.example.eventsourcing.service

import com.example.eventsourcing.config.EventSourcingProperties
import com.example.eventsourcing.config.SnapshottingProperties
import com.example.eventsourcing.domain.Aggregate
import com.example.eventsourcing.domain.AggregateType
import com.example.eventsourcing.domain.event.Event
import com.example.eventsourcing.domain.event.EventWithId
import com.example.eventsourcing.error.OptimisticConcurrencyControlError
import com.example.eventsourcing.repository.AggregateRepository
import com.example.eventsourcing.repository.EventRepository
import org.slf4j.LoggerFactory
import java.util.*

class AggregateStore(
    private val aggregateRepository: AggregateRepository,
    private val eventRepository: EventRepository,
    private val properties: EventSourcingProperties
) {
    fun saveAggregate(aggregate: Aggregate): List<EventWithId<Event>> {
        log.debug("Saving aggregate {}", aggregate)
        aggregateRepository.createAggregateIfAbsent(aggregate.aggregateType, aggregate.aggregateId)

        if (!aggregateRepository.checkAndUpdateAggregateVersion(
                aggregate.aggregateId,
                aggregate.baseVersion,
                aggregate.version
            )) {
            log.warn(
                "Optimistic concurrency control error in aggregate {} {}: " +
                        "actual version doesn't match expected version {}",
                aggregate.aggregateType, aggregate.aggregateId, aggregate.baseVersion
            )
            throw OptimisticConcurrencyControlError(aggregate.baseVersion.toLong())
        }

        return aggregate.changes.map {
            log.info("Appending {} event: {}", aggregate.aggregateType, it)
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
            log.info(
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
    ): Aggregate {
        log.debug("Reading {} aggregate {}", aggregateType, aggregateId)

        val aggregate: Aggregate = if (properties.getSnapshotting(aggregateType).enabled) {
            readAggregateFromSnapshot(aggregateId, version)
                ?: readAggregateFromEvents(aggregateType, aggregateId, version)
                    .also {
                        log.debug("Aggregate {} snapshot not found", aggregateId)
                    }
        } else {
            readAggregateFromEvents(aggregateType, aggregateId, version)
        }
        log.debug("Read aggregate {}", aggregate)

        return aggregate
    }

    private fun readAggregateFromSnapshot(
        aggregateId: UUID,
        aggregateVersion: Int? = null
    ): Aggregate? {
        return aggregateRepository.readAggregateSnapshot(aggregateId, aggregateVersion)
            ?.let { aggregate: Aggregate ->
                log.debug("Read aggregate {} snapshot version {}", aggregateId, aggregate.version)

                if (aggregateVersion == null || aggregate.version < aggregateVersion) {
                    val events = eventRepository.readEvents(aggregateId, aggregate.version, aggregateVersion)
                        .map { it.event }
                        .toList()
                    log.debug(
                        "Read {} events after version {} for aggregate {}",
                        events.size, aggregate.version, aggregateId
                    )
                    aggregate.loadFromHistory(events)
                }
                aggregate
            }
    }

    private fun readAggregateFromEvents(
        aggregateType: AggregateType,
        aggregateId: UUID,
        aggregateVersion: Int? = null
    ): Aggregate {
        val events = eventRepository.readEvents(aggregateId, null, aggregateVersion)
            .map { it.event }

        log.debug("Read {} events for aggregate {}", events.size, aggregateId)
        return aggregateType.newInstance<Aggregate>(aggregateId).also {
            it.loadFromHistory(events)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AggregateStore::class.java)
    }
}
