package com.example.eventsourcing.domain.model

import com.example.eventsourcing.domain.model.command.Command
import com.example.eventsourcing.domain.model.event.Event
import com.fasterxml.jackson.annotation.JsonIgnore
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.function.Consumer

abstract class Aggregate protected constructor(val aggregateId: UUID, var version: Int) {

    @JsonIgnore
    val changes: MutableList<Event> = ArrayList()

    @JsonIgnore
    var baseVersion: Int = version

    abstract val aggregateType: AggregateType

    fun loadFromHistory(events: List<Event>) {
        check(changes.isEmpty()) { "Aggregate has non-empty changes" }
        events.forEach(Consumer { event: Event ->
            check(event.version > version) {
                "Event version ${event.version} <= aggregate base version $nextVersion"
            }
            apply(event)
            version = event.version
            baseVersion = version
        })
    }

    protected val nextVersion: Int
        get() = version + 1

    protected fun applyChange(event: Event) {
        check(event.version == nextVersion) {
            "Event version ${event.version} doesn't match expected version $nextVersion"
        }
        apply(event)
        changes.add(event)
        version = event.version
    }

    private fun apply(event: Event) {
        invoke(event, "apply")
    }

    fun process(command: Command) {
        invoke(command, "process")
    }

    private operator fun invoke(o: Any, methodName: String) {
        try {
            val method = this.javaClass.getMethod(methodName, o.javaClass)
            method.invoke(this, o)
        } catch (e: Exception) {
            throw when(e) {
                is NoSuchMethodException,
                is IllegalAccessException,
                is InvocationTargetException -> UnsupportedOperationException("Aggregate ${this.javaClass} doesn't support ${methodName}(${o.javaClass.simpleName})", e)
                else -> e
            }
        }
    }
}
