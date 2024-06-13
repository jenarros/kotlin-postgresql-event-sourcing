package com.example.eventsourcing.domain.model.event

import java.time.Instant
import java.util.*

sealed class Event(val aggregateId: UUID, val version: Int, val createdAt: Instant) {
    val eventType: EventType = EventType.fromClass(this.javaClass)
}
