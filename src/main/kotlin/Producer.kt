package hello

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

fun main(args: Array<String>) {
    val brokers = args[0]
    val topic = args[1]
    val producer = createProducer(brokers)
    for (i in 0x00..0xFF) {
        val message = "hello $i"
        val futureResult = producer.send(ProducerRecord(topic, message))
        // wait for ack
        futureResult.get()
    }
}

private fun createProducer(brokers: String): Producer<String, String> {
    val props = Properties()
    props["bootstrap.servers"] = brokers
    props["key.serializer"] = StringSerializer::class.java.canonicalName
    props["value.serializer"] = StringSerializer::class.java.canonicalName
    return KafkaProducer<String, String>(props)
}
