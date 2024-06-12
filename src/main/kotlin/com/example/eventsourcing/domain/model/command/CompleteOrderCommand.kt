package com.example.eventsourcing.domain.model.command

import com.example.eventsourcing.domain.model.AggregateType
import java.time.Instant
import java.util.*

class CompleteOrderCommand(
    override val aggregateId: UUID,
    override val createdAt: Instant
) : Command {
    override val aggregateType: AggregateType = AggregateType.ORDER
}
