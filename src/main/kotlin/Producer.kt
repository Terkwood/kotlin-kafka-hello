package hello

import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.apache.kafka.clients.producer.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val brokers = args[0]
    val topic = args[1]
    val producer = createProducer(brokers)

    // maybe the first message takes a while
    producer.send(ProducerRecord(topic, "hello"))

    val elapsedSer = measureTimeMillis {
        for (i in 0x00..0xFF) {
            val message = "ser $i"
            val futureResult = producer.send(ProducerRecord(topic, message))
            // wait for ack
            futureResult.get()
        }
    }
    println("time in serial: $elapsedSer")

    val elapsedCo = measureTimeMillis {
        for (i in 0x00..0xFF) {
            GlobalScope.async {
                val message = "co $i"
                producer.send(ProducerRecord(topic, message))
            }
        }
    }
    println("time in co: $elapsedCo")
}


private fun createProducer(brokers: String): Producer<String, String> {
    val props = Properties()
    props["bootstrap.servers"] = brokers
    props["key.serializer"] = StringSerializer::class.java.canonicalName
    props["value.serializer"] = StringSerializer::class.java.canonicalName
    return KafkaProducer<String, String>(props)
}

suspend inline fun <reified K : Any, reified V : Any> KafkaProducer<K, V>.dispatch(record: ProducerRecord<K, V>) =
    suspendCoroutine<RecordMetadata> { continuation ->
        val callback = Callback { metadata, exception ->
            if (metadata == null) {
                continuation.resumeWithException(exception!!)
            } else {
                continuation.resume(metadata)
            }
        }
        this.send(record, callback)
    }
