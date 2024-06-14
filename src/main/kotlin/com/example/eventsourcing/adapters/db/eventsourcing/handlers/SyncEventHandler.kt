package com.example.eventsourcing.adapters.db.eventsourcing.handlers

import com.example.eventsourcing.domain.Aggregate
import com.example.eventsourcing.domain.AggregateType
import com.example.eventsourcing.domain.model.event.Event
import com.example.eventsourcing.domain.model.event.EventWithId

interface SyncEventHandler {
    fun handleEvents(events: List<EventWithId<Event>>, aggregate: Aggregate)

    val aggregateType: AggregateType
}
