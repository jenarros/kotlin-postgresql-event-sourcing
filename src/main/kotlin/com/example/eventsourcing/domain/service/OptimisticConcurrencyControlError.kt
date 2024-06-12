package com.example.eventsourcing.domain.service

import com.example.eventsourcing.domain.model.Error

class OptimisticConcurrencyControlError(expectedVersion: Long) :
    Error("Actual version doesn't match expected version %s", expectedVersion)
