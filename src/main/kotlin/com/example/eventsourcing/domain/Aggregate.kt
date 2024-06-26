package com.example.eventsourcing.domain

import com.example.eventsourcing.domain.model.event.Event
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

interface Aggregate {
    val aggregateType: AggregateType
    val version: Int
    val baseVersion: Int
    val aggregateId: UUID
    val changes: List<Event>
}
