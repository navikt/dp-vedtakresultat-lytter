package no.nav.dp.vedtakslytter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.coroutines.CoroutineContext

object KafkaLytter : CoroutineScope {
    lateinit var job: Job
    lateinit var config: Configuration
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    fun cancel() {
        job.cancel()
    }

    fun isRunning() = job.isActive

    fun create() {
        this.job = Job()
        this.config = Configuration()
    }

    fun run() {
        launch {
            ProducerConfig.LINGER_MS_CONFIG
            KafkaConsumer<String, String>(config.kafka.toConsumerProps()).use { consumer ->
                consumer.subscribe(listOf(config.kafka.topic))
                while(job.isActive) {
                    val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                    records.forEach { record ->

                    }
                }
            }
        }
    }
}