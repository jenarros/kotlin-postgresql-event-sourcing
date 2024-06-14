package com.example.kotlin.functional

import com.example.eventsourcing.config.Json.jsonify
import com.example.eventsourcing.config.Json.objectMapper
import com.example.eventsourcing.config.Kafka.TOPIC_ORDER_EVENTS
import com.example.eventsourcing.config.Kafka.kafkaConsumer
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.slf4j.LoggerFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.jackson.path
import strikt.jackson.textValue
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.StreamSupport

class OrderTestScript(
    httpHandler: HttpHandler,
    private val kafkaBrokers: String,
    private val integrationKafkaTopic: String = TOPIC_ORDER_EVENTS
) {
    private val riderId: UUID = UUID.randomUUID()
    private val driverId: UUID = UUID.randomUUID()
    private val api = Api(httpHandler)

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
        api.modifyOrder(
            orderId, """
                    {
                      "status":"COMPLETED"
                    }
                    """
        ).expectSuccess()

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
        api.modifyOrder(
            orderId, """
                    {
                      "status":"ACCEPTED",
                      "driverId":"$driverId"
                    }
                    """
        ).expectSuccess()

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
            api.modifyOrder(
                orderId, """
                            {
                              "status":"ADJUSTED",
                              "price":"%s"
                            }
                            
                            """.format(price)
            ).expectSuccess()
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
        val response = api.placeOrder(body).expectSuccess()

        return UUID.fromString(objectMapper.readTree(response.bodyString()).path("orderId").textValue())
    }

    private fun Response.expectSuccess(): Response {
        expectThat(this.status.successful).isTrue()
        return this
    }

    private fun Response.expectBodyToBe(expectedJson: String): Response {
        expectThat(this.bodyString().jsonify()).isEqualTo(expectedJson.jsonify())
        return this
    }

    private fun modifyOrderError(orderId: UUID, body: String, error: String) {
        val response = api.modifyOrder(orderId, body)

        expectThat(response.status.clientError)
            .isTrue()
        val jsonString = response.bodyString()
        expectThat(objectMapper.readTree(jsonString))
            .path("error")
            .textValue()
            .isEqualTo(error)
    }

    private fun getOrder(orderId: UUID, expectedJson: String) =
        api.getOrder(orderId)
            .expectSuccess()
            .expectBodyToBe(expectedJson)

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

    private fun Clock.now(): Instant = Instant.now(this)

    companion object {
        private val log = LoggerFactory.getLogger(OrderTestScript::class.java)
    }
}
