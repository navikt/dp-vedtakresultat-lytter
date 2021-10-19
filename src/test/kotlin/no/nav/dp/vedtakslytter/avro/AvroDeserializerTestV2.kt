package no.nav.dp.vedtakslytter.avro

import de.huxhorn.sulky.ulid.ULID
import no.nav.dp.vedtakslytter.Vedtak
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.EncoderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertNotNull

class AvroDeserializerTestV2 {
    val oslo = ZoneId.of("Europe/Oslo")

    @Test
    fun `can read a v2 object`() {
        val deser = AvroDeserializer()
        val vedtak = Vedtak(
            table = "table",
            opType = "I",
            opTs = ZonedDateTime.now(oslo).minusHours(4),
            currentTs = ZonedDateTime.now(oslo),
            pos = "",
            vedtakId = 2.0,
            vedtakTypeKode = "kjgkjhhjk"
        )
        val vedtakAsGenericRecord = vedtak.toGenericRecordV2()
        val out = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().binaryEncoder(out, null)
        val writer = GenericDatumWriter<GenericRecord>(AvroDeserializer.dagpengeVedtakSchemaV2)
        writer.write(vedtakAsGenericRecord, encoder)
        encoder.flush()
        val record = deser.deserialize("some_topic", out.toByteArray())
        val deserialized = Vedtak.fromGenericRecord(record)
        assertEquals(vedtak.toString(), deserialized.toString())
    }

    @Test
    fun `kan hente vedtak basert på avro melding`() {
        with(Vedtak.fromGenericRecord(lagArenaHendelse())) {
            assertNotNull(this.grunnlagSubsumsjonsId)
            assertNotNull(this.satsSubsumsjonsId)
            assertNotNull(this.periodeSubsumsjonsId)
            assertNotNull(this.minsteInntektSubsumsjonsId)
        }
    }

    private val ulid = ULID()
    private val arenaOpTsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")
    private val arenaCurrentTsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]")
    fun lagArenaHendelse(): GenericData.Record {

        return GenericData.Record(AvroDeserializer.dagpengeVedtakSchemaV2).apply {
            put("table", "table")
            put("op_type", "I")
            put("op_ts", ZonedDateTime.now().format(arenaOpTsFormat))
            put("current_ts", ZonedDateTime.now().format(arenaCurrentTsFormat))
            put("pos", "1")
            put("VEDTAK_ID", 36737638.toDouble())
            put("VEDTAKTYPEKODE", "O")
            put("VEDTAKSTATUSKODE", "IVERK")
            put("UTFALLKODE", "JA")
            put("MINSTEINNTEKT_SUBSUMSJONSID", ulid.nextULID())
            put("PERIODE_SUBSUMSJONSID", ulid.nextULID())
            put("GRUNNLAG_SUBSUMSJONSID", ulid.nextULID())
            put("SATS_SUBSUMSJONSID", ulid.nextULID())
            put("REG_USER", "BB")
            put("REG_DATO", "2020-03-08")
            put("MOD_DATO", "2020-03-08")
            put("MOD_USER", "BB")
        }
    }
}
