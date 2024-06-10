package com.example.kotlin.e2e

import com.example.kotlin.functional.OrderTestScript
import org.http4k.client.ApacheClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.junit.Test

/*
 * Requires having the docker apps running
 */
class PostgreSqlEventSourcingApplicationE2ETests {
    @Test
    fun orderTestScript() {
        val httpHandler = ClientFilters.SetBaseUriFrom(Uri.of(ROOT_URI))
            .then(ApacheClient())
        OrderTestScript(httpHandler, KAFKA_BROKERS).execute()
    }

    companion object {
        private const val ROOT_URI = "http://localhost:8080"
        private const val KAFKA_BROKERS = "localhost:9092"
    }
}
