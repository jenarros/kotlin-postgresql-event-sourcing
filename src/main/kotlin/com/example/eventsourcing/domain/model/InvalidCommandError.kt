package com.example.eventsourcing.domain.model

class InvalidCommandError(message: String) : RuntimeException(message)
