package hello

import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.system.measureTimeMillis

const val SER_MAX = 0xFF
const val CO_MAX = 0xFFFF
const val MSG_PAD = 8
const val TIME_PAD = 4

fun main(args: Array<String>) {
    val brokers = args[0]
    val topic = args[1]
    val producer = createProducer(brokers)

    // maybe the first message takes a while
    producer.send(ProducerRecord(topic, "hello"))

    val elapsedSer = measureTimeMillis {
        for (i in 0x0..SER_MAX) {
            val message = "ser $i"
            val futureResult = producer.send(ProducerRecord(topic, message))
            // wait for ack
            futureResult.get()
        }
    }
    println("time in serial (${SER_MAX.toString().padStart(MSG_PAD)} messages): ${elapsedSer.toString().padStart(
        TIME_PAD)} ms")
    val elapsedCo = measureTimeMillis {
        runBlocking {
            (0x0..CO_MAX).pmap {
                val message = "co $it"
                producer.send(ProducerRecord(topic, message))
            }
        }
    }
    println("time in async  (${CO_MAX.toString().padStart(MSG_PAD)} messages): ${elapsedCo.toString().padStart(TIME_PAD)} ms")
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

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.map { it.await() }
}
