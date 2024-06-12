package com.example.eventsourcing.domain.model

enum class OrderStatus {
    PLACED,
    ADJUSTED,
    ACCEPTED,
    COMPLETED,
    CANCELLED
}
