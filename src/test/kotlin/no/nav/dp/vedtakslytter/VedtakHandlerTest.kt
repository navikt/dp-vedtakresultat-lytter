package no.nav.dp.vedtakslytter

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class VedtakHandlerTest {
    @Test
    fun `Orienterer om brukte subsumsjoner`() {
        val mock = mockk<KafkaProducer<String, String>>(relaxed = true)
        val vedtakHandler = VedtakHandler(mock, "topic")
        vedtakHandler.handleVedtak(nyRettighetMedMinsteInntektSubsumsjon)

        verify(exactly = 2) { mock.send(any(), any()) }
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
        minsteInntektSubsumsjonsId = ulid.nextULID(),
        periodeSubsumsjonsId = ulid.nextULID()
    )

    val grunnlagOgPeriodeSubsumsjon = nyRettighetMedMinsteInntektSubsumsjon.copy(
        minsteInntektSubsumsjonsId = null,
        periodeSubsumsjonsId = null,
        satsSubsumsjonsId = ulid.nextULID(),
        grunnlagSubsumsjonsId = ulid.nextULID()
    )
}
