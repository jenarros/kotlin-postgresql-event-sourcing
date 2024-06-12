package com.example.eventsourcing.domain.model

import java.util.*

enum class AggregateType(val aggregateClass: Class<out com.example.eventsourcing.domain.model.Aggregate>) {
    ORDER(OrderAggregate::class.java);

    fun <T : com.example.eventsourcing.domain.model.Aggregate> newInstance(aggregateId: UUID): T {
        val constructor = aggregateClass.getDeclaredConstructor(
            UUID::class.java, Integer.TYPE
        )
        return constructor.newInstance(aggregateId, 0) as T
    }
}
