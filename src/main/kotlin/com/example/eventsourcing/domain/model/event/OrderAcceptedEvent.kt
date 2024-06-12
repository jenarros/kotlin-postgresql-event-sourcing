package com.example.eventsourcing.domain.model.event

import java.time.Instant
import java.util.*

class OrderAcceptedEvent(
    aggregateId: UUID,
    version: Int,
    createdAt: Instant,
    val driverId: UUID
) : Event(aggregateId, version, createdAt)
