package no.nav.dp.vedtakslytter

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.dp.vedtakslytter.avro.AvroSerializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

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
            topics = listOf(config.kafka.topic, config.kafka.subsumsjonBruktTopic)
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

        val regelApiKlient = mockk<RegelApiKlient>()
        val rapport = slot<SubsumsjonBrukt>()
        every { regelApiKlient.orienterOmSubsumsjon(capture(rapport)) } returns 200
        val testConfig = config.copy(
            kafka = config.kafka.copy(
                bootstrapServer = embeddedEnvironment.brokersURL.substringAfterLast("/"),
                username = username,
                password = password
            )
        )
        KafkaLytter.apply {
            create(config = testConfig, regelApiKlient = regelApiKlient)
            run()
        }
        KafkaProducer<String, GenericRecord>(testConfig.kafka.toProducerProps().apply {
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer::class.java)
            put(ProducerConfig.ACKS_CONFIG, "all")
        }).use { producer ->
            val data = producer.send(
                ProducerRecord(
                    testConfig.kafka.topic,
                    nyRettighetMedMinsteInntektSubsumsjon.vedtakId.toString(),
                    nyRettighetMedMinsteInntektSubsumsjon.toGenericRecord()
                )
            ).get()
            LOGGER.info { "Sent message with offset ${data.offset()}" }
        }
        while (!rapport.isCaptured) {
            delay(1000)
        }
        assertTrue(rapport.isCaptured)

        assertEquals("1337", rapport.captured.eksternId)
    }

    @Test
    fun `Formats double ids correctly`() {
        assertThat(145.5.roundedString()).isEqualTo("145.5")
        assertThat(145.0.roundedString()).isEqualTo("145")
        assertThat(145.55555.roundedString()).isEqualTo("145.55555")
    }
    private val nyRettighetMedMinsteInntektSubsumsjon = Vedtak(
        vedtakId = 1337.0,
        table = "",
        opType = "I",
        opTs = ZonedDateTime.now().minusHours(6),
        currentTs = ZonedDateTime.now().minusHours(3),
        pos = "",
        primaryKeys = listOf("VEDTAKID"),
        vedtakTypeKode = "O",
        vedtakStatusKode = "IVERK",
        minsteInntektSubsumsjonsId = UUID.randomUUID().toString()
    )
}