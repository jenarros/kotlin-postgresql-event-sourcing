package com.example.eventsourcing.domain.model.command

import com.example.eventsourcing.domain.model.AggregateType
import java.time.Instant
import java.util.*

interface Command {
    val aggregateType: AggregateType
    val aggregateId: UUID
    val createdAt: Instant
}
