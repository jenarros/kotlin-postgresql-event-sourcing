package com.example.eventsourcing.domain.service.event

import com.example.eventsourcing.domain.model.Aggregate
import com.example.eventsourcing.domain.model.AggregateType
import com.example.eventsourcing.domain.model.event.Event
import com.example.eventsourcing.domain.model.event.EventWithId

interface SyncEventHandler {
    fun handleEvents(events: List<EventWithId<Event>>, aggregate: com.example.eventsourcing.domain.model.Aggregate)

    val aggregateType: AggregateType
}
