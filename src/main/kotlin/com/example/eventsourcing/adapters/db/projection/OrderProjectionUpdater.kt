package com.example.eventsourcing.adapters.db.projection

import com.example.eventsourcing.adapters.db.projection.OrderProjectionMapper.toProjection
import com.example.eventsourcing.domain.AggregateType
import com.example.eventsourcing.domain.OrderAggregate
import com.example.eventsourcing.domain.model.event.Event
import com.example.eventsourcing.domain.model.event.EventWithId
import com.example.eventsourcing.adapters.db.eventsourcing.handlers.SyncEventHandler
import com.example.eventsourcing.domain.Aggregate
import org.slf4j.Logger
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

class OrderProjectionUpdater(
    private val repository: JpaRepository<OrderProjection, UUID>,
    private val log: Logger
) : SyncEventHandler {
    override fun handleEvents(events: List<EventWithId<Event>>, aggregate: Aggregate) {
        log.debug("Updating read model for order {}", aggregate)
        updateOrderProjection(aggregate as OrderAggregate)
    }

    private fun updateOrderProjection(orderAggregate: OrderAggregate) {
        val orderProjection = toProjection(orderAggregate)
        log.info("Saving order projection {}", orderProjection)
        repository.save(orderProjection)
    }

    override val aggregateType: AggregateType = AggregateType.ORDER
}
