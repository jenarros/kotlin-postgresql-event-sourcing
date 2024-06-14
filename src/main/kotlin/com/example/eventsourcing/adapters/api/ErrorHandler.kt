package com.example.eventsourcing.adapters.api

import com.example.eventsourcing.domain.model.InvalidCommandError
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import java.io.PrintWriter
import java.io.StringWriter

object ErrorHandler : (Throwable) -> Response {
    private val errorMessageLens = Body.auto<ErrorMessage>().toLens()

    override fun invoke(e: Throwable): Response {
        e.printStackTrace()
        return when (e) {
            is InvalidCommandError -> Response(Status.BAD_REQUEST)
                .with(errorMessageLens of ErrorMessage((e.message ?: "").format(e)))

            else -> {
                if (e !is Exception) throw e

                val stackTraceAsString = StringWriter().apply {
                    e.printStackTrace(PrintWriter(this))
                }.toString()

                Response(Status.INTERNAL_SERVER_ERROR).body(stackTraceAsString)
            }
        }
    }
}
