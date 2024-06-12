package com.example.eventsourcing.domain.service.event

import com.example.eventsourcing.domain.model.AggregateType
import com.example.eventsourcing.domain.model.event.Event
import com.example.eventsourcing.domain.model.event.EventWithId

interface AsyncEventHandler {
    fun handleEvent(eventWithId: EventWithId<Event>)
    fun subscriptionName(): String = javaClass.name

    val aggregateType: AggregateType
}
