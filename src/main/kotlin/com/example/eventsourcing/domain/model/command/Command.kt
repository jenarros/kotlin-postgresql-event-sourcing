package com.example.eventsourcing.domain.model.command

import com.example.eventsourcing.domain.AggregateType
import java.time.Instant
import java.util.*

sealed interface Command {
    val aggregateType: AggregateType
    val aggregateId: UUID
    val createdAt: Instant
}
