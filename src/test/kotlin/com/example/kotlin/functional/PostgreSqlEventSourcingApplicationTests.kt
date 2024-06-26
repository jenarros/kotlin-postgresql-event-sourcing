package com.example.kotlin.functional

import com.example.eventsourcing.config.Kafka.TOPIC_ORDER_EVENTS
import com.example.kotlin.functional.TestEnvironment.kafka
import org.junit.jupiter.api.Test
import java.util.*

class PostgreSqlEventSourcingApplicationTests {

    @Test
    fun orderTestScript() {
        val topic = TOPIC_ORDER_EVENTS + "-" + UUID.randomUUID()
        OrderTestScript(
            TestEnvironment.newApp(topic),
            kafka.bootstrapServers,
            topic
        ).execute()
    }
}
