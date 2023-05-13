package com.example.kotlin.functional

import com.example.eventsourcing.TOPIC_ORDER_EVENTS
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions
import org.slf4j.LoggerFactory
import org.springframework.boot.test.json.BasicJsonTester
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.math.BigDecimal
import java.time.Duration
import java.util.*
import java.util.stream.StreamSupport

class OrderTestScript(private val restTemplate: TestRestTemplate, private val kafkaBrokers: String) {
    private val objectMapper = ObjectMapper()
    private val jsonTester = BasicJsonTester(javaClass)
    fun execute() {
        log.info("Place a new order")
        val orderId = placeOrder(
            """
                {
                  "riderId":"63770803-38f4-4594-aec2-4c74918f7165",
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
                
                """.trimIndent()
        )
        log.info("Get the placed order")
        getOrder(
            orderId, """
                {
                  "id":"%s",
                  "version":1,
                  "status":"PLACED",
                  "riderId":"63770803-38f4-4594-aec2-4c74918f7165",
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
                  ]
                }
                
                """.trimIndent().formatted(orderId)
        )
        var price = BigDecimal("100.00")
        for (i in 0..19) {
            price = price.add(BigDecimal("10"))
            log.info("Adjust the order")
            modifyOrder(
                orderId, """
                    {
                      "status":"ADJUSTED",
                      "price":"%s"
                    }
                    
                    """.trimIndent().formatted(price)
            )
        }
        log.info("Get the adjusted order")
        getOrder(
            orderId, """
                {
                  "id":"%s",
                  "version":21,
                  "status":"ADJUSTED",
                  "riderId":"63770803-38f4-4594-aec2-4c74918f7165",
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
                  ]
                }
                
                """.trimIndent().formatted(orderId)
        )
        log.info("Accepted the order")
        modifyOrder(
            orderId, """
                {
                  "status":"ACCEPTED",
                  "driverId":"2c068a1a-9263-433f-a70b-067d51b98378"
                }
                
                """.trimIndent()
        )
        log.info("Get the accepted order")
        getOrder(
            orderId, """
                {
                  "id":"%s",
                  "version":22,
                  "status":"ACCEPTED",
                  "riderId":"63770803-38f4-4594-aec2-4c74918f7165",
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
                  "driverId":"2c068a1a-9263-433f-a70b-067d51b98378"
                }
                
                """.trimIndent().formatted(orderId)
        )
        log.info("Complete the order")
        modifyOrder(
            orderId, """
                {
                  "status":"COMPLETED"
                }
                
                """.trimIndent()
        )
        log.info("Get the completed order")
        getOrder(
            orderId, """
                {
                  "id":"%s",
                  "version":23,
                  "status":"COMPLETED",
                  "riderId":"63770803-38f4-4594-aec2-4c74918f7165",
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
                  "driverId":"2c068a1a-9263-433f-a70b-067d51b98378"
                }
                
                """.trimIndent().formatted(orderId)
        )
        log.info("Try to cancel the completed order")
        modifyOrderError(
            orderId, """
                {
                  "status":"CANCELLED"
                }
                
                """.trimIndent(), "Order in status COMPLETED can't be cancelled"
        )
        log.info("Print integration events")
        val kafkaConsumer = createKafkaConsumer(TOPIC_ORDER_EVENTS)
        val kafkaRecordValues = getKafkaRecords(kafkaConsumer, Duration.ofSeconds(10), 23)
        Assertions.assertThat(kafkaRecordValues)
            .hasSizeGreaterThanOrEqualTo(23)
        val lastKafkaRecordValue = kafkaRecordValues[kafkaRecordValues.size - 1]
        Assertions.assertThat(jsonTester.from(lastKafkaRecordValue)).isEqualToJson(
            """
                {
                  "order_id":"%s",
                  "event_type":"ORDER_COMPLETED",
                  "version":23,
                  "status":"COMPLETED",
                  "rider_id":"63770803-38f4-4594-aec2-4c74918f7165",
                  "price":300.00,
                  "route":[
                    {
                      "lat":50.51980052414157,
                      "lon":30.467197278948536,
                      "address":"Kyiv, 17A Polyarna Street"
                    },
                    {
                      "lat":50.48509161169076,
                      "lon":30.485170724431292,
                      "address":"Kyiv, 18V Novokostyantynivska Street"
                    }
                  ],
                  "driver_id":"2c068a1a-9263-433f-a70b-067d51b98378"
                }
                
                """.trimIndent().formatted(orderId)
        )
    }

    private fun placeOrder(body: String): UUID {
        val response = restTemplate.exchange(
            "/orders",
            HttpMethod.POST,
            HttpEntity(body, HEADERS),
            String::class.java
        )
        Assertions.assertThat(response.statusCode.is2xxSuccessful)
            .isTrue()
        val jsonString = response.body
        Assertions.assertThat(jsonTester.from(jsonString))
            .extractingJsonPathStringValue("@.orderId")
            .isNotEmpty()
        return try {
            val jsonTree = objectMapper.readTree(jsonString)
            val orderId = jsonTree["orderId"].asText()
            UUID.fromString(orderId)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    private fun modifyOrder(orderId: UUID, body: String) {
        val response = restTemplate.exchange(
            "/orders/$orderId",
            HttpMethod.PUT,
            HttpEntity(body, HEADERS),
            String::class.java
        )
        Assertions.assertThat(response.statusCode.is2xxSuccessful)
            .isTrue()
    }

    private fun modifyOrderError(orderId: UUID, body: String, error: String) {
        val response = restTemplate.exchange(
            "/orders/$orderId",
            HttpMethod.PUT,
            HttpEntity(body, HEADERS),
            String::class.java
        )
        Assertions.assertThat(response.statusCode.is4xxClientError)
            .isTrue()
        val jsonString = response.body
        Assertions.assertThat(jsonTester.from(jsonString))
            .extractingJsonPathStringValue("@.error")
            .isEqualTo(error)
    }

    private fun getOrder(orderId: UUID, expectedJson: String) {
        val response = restTemplate.exchange(
            "/orders/$orderId",
            HttpMethod.GET,
            HttpEntity<Any>(HEADERS),
            String::class.java
        )
        Assertions.assertThat(response.statusCode.is2xxSuccessful)
            .isTrue()
        val jsonString = response.body
        Assertions.assertThat(jsonTester.from(jsonString))
            .isEqualToJson(expectedJson)
    }

    private fun createKafkaConsumer(vararg topicsToConsume: String): Consumer<String, String> {
        val consumerProps = KafkaTestUtils.consumerProps(
            kafkaBrokers,
            this.javaClass.simpleName + "-consumer",
            "true"
        )
        val cf = DefaultKafkaConsumerFactory(
            consumerProps,
            StringDeserializer(),
            StringDeserializer()
        )
        val consumer = cf.createConsumer()
        consumer.subscribe(Arrays.asList(*topicsToConsume))
        return consumer
    }

    private fun getKafkaRecords(
        consumer: Consumer<String, String>,
        timeout: Duration,
        minRecords: Int
    ): List<String> {
        val records = KafkaTestUtils.getRecords(consumer, timeout, minRecords)
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(records.iterator(), Spliterator.ORDERED), false)
            .sorted(Comparator.comparingLong { obj: ConsumerRecord<String, String> -> obj.timestamp() })
            .map { obj: ConsumerRecord<String, String> -> obj.value() }
            .toList()
    }

    companion object {
        private val HEADERS = HttpHeaders()
        private val log = LoggerFactory.getLogger(OrderTestScript::class.java)

        init {
            HEADERS.contentType =
                MediaType.APPLICATION_JSON
            HEADERS.accept =
                java.util.List.of(MediaType.APPLICATION_JSON)
        }
    }
}
