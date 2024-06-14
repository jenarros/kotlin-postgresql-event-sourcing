package com.example.eventsourcing.adapters.db.eventsourcing

class OptimisticConcurrencyControlError(expectedVersion: Long) :
    RuntimeException("Actual version doesn't match expected version $expectedVersion")
