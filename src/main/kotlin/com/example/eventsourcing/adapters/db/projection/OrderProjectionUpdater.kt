package com.example.eventsourcing.adapters.db.projection

import com.example.eventsourcing.adapters.db.projection.OrderProjectionMapper.toProjection
import com.example.eventsourcing.domain.model.AggregateType
import com.example.eventsourcing.domain.model.OrderAggregate
import com.example.eventsourcing.domain.model.event.Event
import com.example.eventsourcing.domain.model.event.EventWithId
import com.example.eventsourcing.domain.service.event.SyncEventHandler
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

class OrderProjectionUpdater(private val repository: JpaRepository<OrderProjection, UUID>) : SyncEventHandler {
    override fun handleEvents(events: List<EventWithId<Event>>, aggregate: com.example.eventsourcing.domain.model.Aggregate) {
        log.debug("Updating read model for order {}", aggregate)
        updateOrderProjection(aggregate as OrderAggregate)
    }

    private fun updateOrderProjection(orderAggregate: OrderAggregate) {
        val orderProjection = toProjection(orderAggregate)
        log.info("Saving order projection {}", orderProjection)
        repository.save(orderProjection)
    }

    override val aggregateType: AggregateType = AggregateType.ORDER

    companion object {
        private val log = LoggerFactory.getLogger(OrderProjectionUpdater::class.java)
    }
}
