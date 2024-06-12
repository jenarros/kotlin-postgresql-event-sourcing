package com.example.eventsourcing.domain.model.event

import java.time.Instant
import java.util.*

class OrderCompletedEvent(
    aggregateId: UUID,
    version: Int,
    createdAt: Instant,
) : Event(aggregateId, version, createdAt)
