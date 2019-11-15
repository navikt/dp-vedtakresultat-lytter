package no.nav.dp.vedtakslytter

import de.huxhorn.sulky.ulid.ULID
import kotlinx.coroutines.runBlocking
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.dp.vedtakslytter.avro.AvroSerializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZonedDateTime

class KafkaLytterTest {
    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"
        val config = Configuration()
        val embeddedEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = false,
            withSchemaRegistry = false,
            withSecurity = true,
            topicInfos = listOf(
                KafkaEnvironment.TopicInfo(config.kafka.topic), KafkaEnvironment.TopicInfo(config.kafka.subsumsjonBruktTopic))
        )

        @BeforeAll
        @JvmStatic
        fun setup() {
            embeddedEnvironment.start()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            embeddedEnvironment.tearDown()
        }
    }

    @Test
    fun `embedded kafka cluster managed to start`() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }

    @Test
    fun `minsteinntekts subsumsjon blir sagt fra om`() = runBlocking {

        val testConfig = config.copy(
            kafka = config.kafka.copy(
                bootstrapServer = embeddedEnvironment.brokersURL.substringAfterLast("/"),
                username = username,
                password = password
            )
        )
        KafkaLytter.apply {
            create(config = testConfig)
            run()
        }
        KafkaProducer<String, GenericRecord>(testConfig.kafka.toProducerProps().apply {
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer::class.java)
            put(ProducerConfig.ACKS_CONFIG, "all")
        }).use { producer ->
            producer.send(
                ProducerRecord(
                    testConfig.kafka.topic,
                    nyRettighetMedMinsteInntektSubsumsjon.vedtakId.toString(),
                    nyRettighetMedMinsteInntektSubsumsjon.toGenericRecord()
                )
            ).get()
        }
        var messagesRead = 0
        KafkaConsumer<String, String>(testConfig.kafka.toConsumerProps().apply {
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        }).use { consumer ->
            consumer.subscribe(listOf(testConfig.kafka.subsumsjonBruktTopic))
            while (messagesRead == 0) {
                val records = consumer.poll(Duration.ofSeconds(2))
                messagesRead += records.count()
                records.asSequence().map { r -> subsumsjonAdapter.fromJson(r.value()) }.forEach { sub ->
                    assertEquals("1337", sub?.eksternId)
                }
            }
        }
        assertTrue(messagesRead > 0)
    }

    @Test
    fun `Formats double ids correctly`() {
        145.5.roundedString() shouldBe "145.5"
        145.0.roundedString() shouldBe "145"
        145.55555.roundedString() shouldBe "145.55555"
    }
    val ulid = ULID()
    val nyRettighetMedMinsteInntektSubsumsjon = Vedtak(
        vedtakId = 1337.0,
        table = "",
        opType = "I",
        opTs = ZonedDateTime.now().minusHours(6),
        currentTs = ZonedDateTime.now().minusHours(3),
        pos = "",
        primaryKeys = listOf("VEDTAKID"),
        vedtakTypeKode = "O",
        vedtakStatusKode = "IVERK",
        minsteInntektSubsumsjonsId = ulid.nextULID()
    )
}