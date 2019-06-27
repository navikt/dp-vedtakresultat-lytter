package no.nav.dp.vedtakslytter.avro

import no.nav.dp.vedtakslytter.Vedtak
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.EncoderFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.Instant

class AvroDeserializerTest {

    @Test
    fun `can read an object`() {
        val deser = AvroDeserializer()
        val vedtak = Vedtak(
            table = "table",
            opType = "I",
            opTs = "-1",
            currentTs = Instant.now().toEpochMilli().toString(),
            pos = "",
            primaryKeys = listOf("vedtakId"),
            tokens = mapOf("test" to "token"),
            vedtakId = 2.0,
            vedtakTypeKode = "kjgkjhhjk"
        )
        val vedtakAsGenericRecord = vedtak.toGenericRecord()
        val out = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().binaryEncoder(out, null)
        val writer = GenericDatumWriter<GenericRecord>(AvroDeserializer.schema)
        writer.write(vedtakAsGenericRecord, encoder)
        encoder.flush()
        val record = deser.deserialize("some_topic", out.toByteArray())
        val deserialized = Vedtak.fromGenericRecord(record)
        assertEquals(vedtak.toString(), deserialized.toString())
    }
}