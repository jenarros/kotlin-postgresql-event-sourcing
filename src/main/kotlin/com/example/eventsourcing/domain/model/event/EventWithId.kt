package com.example.eventsourcing.domain.model.event

import java.math.BigInteger

data class EventWithId<T : Event>(val id: Long, val transactionId: BigInteger, val event: T)
