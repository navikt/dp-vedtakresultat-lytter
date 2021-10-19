package no.nav.dp.vedtakslytter.avro

import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.EncoderFactory
import org.apache.kafka.common.serialization.Serializer
import java.io.ByteArrayOutputStream

class AvroSerializer : Serializer<GenericRecord> {
    companion object {
        val schema = Schema.Parser().parse("GRENSESNITT.FERDIGSTILTE_DAGPENGEVEDTAK_V1.avsc".toInputStream())
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
    }

    override fun serialize(topic: String?, data: GenericRecord): ByteArray {
        val writer = GenericDatumWriter<GenericRecord>(schema)
        val baos = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().binaryEncoder(baos, null)
        writer.write(data, encoder)
        encoder.flush()
        return baos.toByteArray()
    }

    override fun close() {
    }
}
