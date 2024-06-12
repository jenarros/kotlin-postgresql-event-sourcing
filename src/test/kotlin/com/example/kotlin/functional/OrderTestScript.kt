package com.example.kotlin.functional

import com.example.eventsourcing.config.Json
import com.example.eventsourcing.config.Json.jsonify
import com.example.eventsourcing.config.Json.objectMapper
import com.example.eventsourcing.config.Kafka.TOPIC_ORDER_EVENTS
import com.example.eventsourcing.config.Kafka.kafkaConsumer
import com.fasterxml.jackson.core.JsonProcessingException
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.slf4j.LoggerFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.jackson.isTextual
import strikt.jackson.path
import strikt.jackson.textValue
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.StreamSupport

class OrderTestScript(
    private val httpHandler: HttpHandler,
    private val kafkaBrokers: String,
    private val integrationKafkaTopic: String = TOPIC_ORDER_EVENTS
) {
    private val riderId: UUID = UUID.randomUUID()
    private val driverId: UUID = UUID.randomUUID()

    fun execute() {
        kafkaConsumer(kafkaBrokers, listOf(integrationKafkaTopic)).use { kafkaConsumer ->
            val orderId = placeNewOrder(riderId)
            adjustOrder(orderId)
            acceptTheOrder(orderId)
            completeTheOrder(orderId)
            tryToCancelTheCompletedOrder(orderId)
            verifyIntegrationEvents(orderId, kafkaConsumer)
        }
    }

    fun verifyIntegrationEvents(orderId: UUID, kafkaConsumer: Consumer<String, String>) {
        log.info("Print integration events")
        val kafkaRecordValues = getKafkaRecords(kafkaConsumer, Duration.ofSeconds(30), 23)

        val lastKafkaRecordValue = kafkaRecordValues[kafkaRecordValues.size - 1]
        expectThat(lastKafkaRecordValue.jsonify()).isEqualTo(
            """
                    {
                      "orderId":"%s",
                      "eventType":"ORDER_COMPLETED",
                      "eventTimestamp": ${TestEnvironment.clock.instant().toEpochMilli()},
                      "version":23,
                      "status":"COMPLETED",
                      "riderId":"$riderId",
                      "price":300.00,
                      "route":[
                        {
                          "address":"Kyiv, 17A Polyarna Street",
                          "lat":50.51980052414157,
                          "lon":30.467197278948536
                        },
                        {
                          "address":"Kyiv, 18V Novokostyantynivska Street",
                          "lat":50.48509161169076,
                          "lon":30.485170724431292
                        }
                      ],
                      "driverId":"$driverId"
                    }
                    """.format(orderId).jsonify()
        )
    }

    private fun tryToCancelTheCompletedOrder(orderId: UUID) {
        log.info("Try to cancel the completed order")
        modifyOrderError(
            orderId, """
                    {
                      "status":"CANCELLED"
                    }
                    """, "Order in status COMPLETED can't be cancelled"
        )
    }

    private fun completeTheOrder(orderId: UUID) {
        log.info("Complete the order")
        modifyOrder(
            orderId, """
                    {
                      "status":"COMPLETED"
                    }
                    """
        )

        log.info("Get the completed order")
        getOrder(
            orderId, """
                    {
                      "id":"%s",
                      "version":23,
                      "status":"COMPLETED",
                      "riderId":"$riderId",
                      "price":300.00,
                      "route":[
                        {
                          "address":"Kyiv, 17A Polyarna Street",
                          "lat":50.51980052414157,
                          "lon":30.467197278948536
                        },
                        {
                          "address":"Kyiv, 18V Novokostyantynivska Street",
                          "lat":50.48509161169076,
                          "lon":30.485170724431292
                        }
                      ],
                      "driverId":"$driverId",
                      "placedDate": "${TestEnvironment.clock.now()}",
                      "acceptedDate": "${TestEnvironment.clock.now()}",
                      "completedDate": "${TestEnvironment.clock.now()}"
                    }
                    """.format(orderId)
        )
    }

    private fun acceptTheOrder(orderId: UUID) {
        log.info("Accepted the order")
        modifyOrder(
            orderId, """
                    {
                      "status":"ACCEPTED",
                      "driverId":"$driverId"
                    }
                    """
        )

        log.info("Get the accepted order")
        getOrder(
            orderId, """
                    {
                      "id":"%s",
                      "version":22,
                      "status":"ACCEPTED",
                      "riderId":"$riderId",
                      "price":300.00,
                      "route":[
                        {
                          "address":"Kyiv, 17A Polyarna Street",
                          "lat":50.51980052414157,
                          "lon":30.467197278948536
                        },
                        {
                          "address":"Kyiv, 18V Novokostyantynivska Street",
                          "lat":50.48509161169076,
                          "lon":30.485170724431292
                        }
                      ],
                      "driverId":"$driverId",
                      "placedDate" : "${TestEnvironment.clock.now()}",
                      "acceptedDate" : "${TestEnvironment.clock.now()}"
                    }
                    """.format(orderId)
        )
    }

    private fun adjustOrder(orderId: UUID) {
        log.info("Adjust the order")

        var price = BigDecimal("100.00")
        for (i in 0..19) {
            price = price.add(BigDecimal("10"))
            modifyOrder(
                orderId, """
                        {
                          "status":"ADJUSTED",
                          "price":"%s"
                        }
                        
                        """.format(price)
            )
        }

        log.info("Get the adjusted order")
        getOrder(
            orderId, """
                    {
                      "id":"%s",
                      "version":21,
                      "status":"ADJUSTED",
                      "riderId":"$riderId",
                      "price":300.00,
                      "route":[
                        {
                          "address":"Kyiv, 17A Polyarna Street",
                          "lat":50.51980052414157,
                          "lon":30.467197278948536
                        },
                        {
                          "address":"Kyiv, 18V Novokostyantynivska Street",
                          "lat":50.48509161169076,
                          "lon":30.485170724431292
                        }
                      ],
                      "placedDate" : "${TestEnvironment.clock.now()}"
                    }
                    """.format(orderId)
        )
    }

    fun placeNewOrder(riderId: UUID): UUID {
        log.info("Place a new order")

        val orderId = placeOrder(
            """
                    {
                      "riderId":"$riderId",
                      "price":"123.45",
                      "route":[
                        {
                          "address":"Kyiv, 17A Polyarna Street",
                          "lat":50.51980052414157,
                          "lon":30.467197278948536
                        },
                        {
                          "address":"Kyiv, 18V Novokostyantynivska Street",
                          "lat":50.48509161169076,
                          "lon":30.485170724431292
                        }
                      ]
                    }
                    """
        )

        getOrder(
            orderId, """
                    {
                      "id":"%s",
                      "version":1,
                      "status":"PLACED",
                      "riderId":"$riderId",
                      "price":123.45,
                      "route":[
                        {
                          "address":"Kyiv, 17A Polyarna Street",
                          "lat":50.51980052414157,
                          "lon":30.467197278948536
                        },
                        {
                          "address":"Kyiv, 18V Novokostyantynivska Street",
                          "lat":50.48509161169076,
                          "lon":30.485170724431292
                        }
                      ],
                      "placedDate" : "${TestEnvironment.clock.now()}"
                    }
                    """.format(orderId)
        )

        return orderId
    }

    private fun placeOrder(body: String): UUID {
        val response = httpHandler(
            Request(Method.POST, "/orders")
                .json()
                .body(body)
        )
        expectThat(response.status.successful)
            .isTrue()
        val jsonString = response.bodyString()
        expectThat(Json.objectMapper.readTree(jsonString))
            .path("orderId")
            .isTextual()

        return try {
            val jsonTree = objectMapper.readTree(jsonString)
            val orderId = jsonTree["orderId"].asText()
            UUID.fromString(orderId)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    private fun modifyOrder(orderId: UUID, body: String) {
        val response = httpHandler(
            Request(Method.PUT, "/orders/$orderId")
                .json()
                .body(body)
        )
        expectThat(response.status.successful)
            .isTrue()
    }

    private fun modifyOrderError(orderId: UUID, body: String, error: String) {
        val response = httpHandler(
            Request(Method.PUT, "/orders/$orderId")
                .json()
                .body(body)
        )
        expectThat(response.status.clientError)
            .isTrue()
        val jsonString = response.bodyString()
        expectThat(objectMapper.readTree(jsonString))
            .path("error")
            .textValue()
            .isEqualTo(error)
    }

    private fun getOrder(orderId: UUID, expectedJson: String) {
        val response = httpHandler(
            Request(Method.GET, "/orders/$orderId")
                .json()
        )
        expectThat(response.status.successful)
            .isTrue()
        expectThat(response.bodyString().jsonify()).isEqualTo(expectedJson.jsonify())
    }

    fun getKafkaRecords(
        consumer: Consumer<String, String>,
        timeout: Duration,
        minRecords: Int
    ): List<String> {
        val records = mutableListOf<ConsumerRecord<String, String>>()

        do {
            records.addAll(consumer.poll(timeout).map { it })
        } while (records.size < minRecords)

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(records.iterator(), Spliterator.ORDERED), false)
            .sorted(Comparator.comparingLong { obj: ConsumerRecord<String, String> -> obj.timestamp() })
            .map { obj: ConsumerRecord<String, String> -> obj.value() }
            .toList()
    }

    companion object {
        fun Request.json() =
            header("content-type", "application/json")
                .header("accept", "application/json")

        private val log = LoggerFactory.getLogger(OrderTestScript::class.java)
    }
}

private fun Clock.now(): Instant = Instant.now(this)
