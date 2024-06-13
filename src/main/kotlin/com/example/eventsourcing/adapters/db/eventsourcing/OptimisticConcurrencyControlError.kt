package com.example.eventsourcing.adapters.db.eventsourcing

import com.example.eventsourcing.domain.model.Error

class OptimisticConcurrencyControlError(expectedVersion: Long) :
    Error("Actual version doesn't match expected version %s", expectedVersion)
