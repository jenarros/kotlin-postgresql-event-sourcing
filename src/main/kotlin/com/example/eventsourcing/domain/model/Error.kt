package com.example.eventsourcing.domain.model

open class Error(message: String, vararg args: Any?) : RuntimeException(message.format(*args))
