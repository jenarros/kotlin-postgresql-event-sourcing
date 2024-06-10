//package jenm.examples.kafka
//
//import io.confluent.kafka.serializers.KafkaJsonSerializer
//import jenm.examples.kafka.util.createTopic
//import jenm.examples.kafka.model.DataRecord
//import jenm.examples.kafka.util.loadKafkaConfig
//import jenm.examples.kafka.util.logMessage
//import org.apache.kafka.clients.producer.KafkaProducer
//import org.apache.kafka.clients.producer.ProducerConfig.*
//import org.apache.kafka.clients.producer.ProducerRecord
//import org.apache.kafka.clients.producer.RecordMetadata
//import org.apache.kafka.common.serialization.StringSerializer
//import java.time.LocalDateTime
//
//fun producer(topic: String) {
//    val kafkaProps = loadKafkaConfig()
//
//    // Create topic if needed
//    createTopic(topic, 1, 1, kafkaProps)
//
//    // Add additional properties.
//    kafkaProps[ACKS_CONFIG] = "all"
//    kafkaProps[KEY_SERIALIZER_CLASS_CONFIG] = "${StringSerializer::class.qualifiedName}"
//    kafkaProps[VALUE_SERIALIZER_CLASS_CONFIG] = "${KafkaJsonSerializer::class.qualifiedName}"
//
//    // Produce sample data
//    val numMessages = 10
//    KafkaProducer<String, DataRecord>(kafkaProps).use { producer ->
//        repeat(numMessages) { i ->
//            val key = "messageKey" // key is used by kafka to maintain order and it is optional
//            val record = DataRecord("Message ${(i + 1).toLong()}", LocalDateTime.now().toString())
//
//            producer.send(ProducerRecord(topic, key, record)) { m: RecordMetadata, e: Exception? ->
//                when (e) {
//                    // no exception, good to go!
//                    null -> logMessage("Produced record $record to topic ${m.topic()} partition [${m.partition()}] @ offset ${m.offset()}")
//                    // print stacktrace in case of exception
//                    else -> e.printStackTrace()
//                }
//            }
//        }
//
//        producer.flush()
//        logMessage("$numMessages messages were produced to topic $topic")
//    }
//}
//
//
