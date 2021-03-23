package no.nav.dp.vedtakslytter

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.concurrent.Future

class VedtakHandlerTest {
    @Test
    fun `Orienterer om brukte minsteinntekt og periodesubsumsjoner`() {
        val slots = mutableListOf<ProducerRecord<String, String>>()
        val mock = mockk<KafkaProducer<String, String>>().also {
            every {
                it.send(
                    capture(slots),
                    any()
                )
            } returns mockk<Future<RecordMetadata>>()
        }
        val vedtakHandler = VedtakHandler(mock, "topic")
        vedtakHandler.handleVedtak(nyRettighetMedMinsteInntektOgPeriodeSubsumsjon)
        vedtakHandler.handleVedtak(grunnlagOgSatsSubsumsjon)

        slots.size shouldBe 4
        val resultater = slots.map {
            subsumsjonAdapter.fromJson(it.value())!!
        }

        resultater.shouldHaveSingleElement {
            s ->
            s.id == nyRettighetMedMinsteInntektOgPeriodeSubsumsjon.minsteInntektSubsumsjonsId
        }
        resultater.shouldHaveSingleElement {
            s ->
            s.id == nyRettighetMedMinsteInntektOgPeriodeSubsumsjon.periodeSubsumsjonsId
        }
        resultater.shouldHaveSingleElement {
            s ->
            s.id == grunnlagOgSatsSubsumsjon.grunnlagSubsumsjonsId
        }
        resultater.shouldHaveSingleElement {
            s ->
            s.id == grunnlagOgSatsSubsumsjon.satsSubsumsjonsId
        }
    }

    @Test
    fun `Formats double ids correctly`() {
        145.5.roundedString() shouldBe "145.5"
        145.0.roundedString() shouldBe "145"
        145.55555.roundedString() shouldBe "145.55555"
    }

    val ulid = ULID()
    val nyRettighetMedMinsteInntektOgPeriodeSubsumsjon = Vedtak(
        vedtakId = 1337.0,
        table = "",
        opType = "I",
        opTs = ZonedDateTime.now().minusHours(6),
        currentTs = ZonedDateTime.now().minusHours(3),
        pos = "",
        primaryKeys = listOf("VEDTAKID"),
        vedtakTypeKode = "O",
        vedtakStatusKode = "IVERK",
        minsteInntektSubsumsjonsId = ulid.nextULID(),
        periodeSubsumsjonsId = ulid.nextULID()
    )

    val grunnlagOgSatsSubsumsjon = nyRettighetMedMinsteInntektOgPeriodeSubsumsjon.copy(
        minsteInntektSubsumsjonsId = null,
        periodeSubsumsjonsId = null,
        satsSubsumsjonsId = ulid.nextULID(),
        grunnlagSubsumsjonsId = ulid.nextULID()
    )
}
