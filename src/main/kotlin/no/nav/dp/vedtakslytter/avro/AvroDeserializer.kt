package no.nav.dp.vedtakslytter.avro

import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.kafka.common.serialization.Deserializer
import java.io.IOException
import java.io.InputStream

class AvroDeserializer : Deserializer<GenericRecord> {
    companion object {
        val dagpengeVedtakSchemaV1 = Schema.Parser().parse("GRENSESNITT.FERDIGSTILTE_DAGPENGEVEDTAK_V1.avsc".toInputStream())
        val dagpengeVedtakSchemaV2 = Schema.Parser().parse("GRENSESNITT.FERDIGSTILTE_DAGPENGEVEDTAK_V2.avsc".toInputStream())

        val dagpengeVedtakReaderV1 = GenericDatumReader<GenericRecord>(dagpengeVedtakSchemaV1)
        val dagpengeVedtakReaderV2 = GenericDatumReader<GenericRecord>(dagpengeVedtakSchemaV2)
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
    }

    override fun deserialize(topic: String, data: ByteArray): GenericRecord {
        return try {
            dagpengeVedtakReaderV2.read(null, DecoderFactory.get().binaryDecoder(data, null))
        } catch(e: IOException) {
            dagpengeVedtakReaderV1.read(null, DecoderFactory.get().binaryDecoder(data, null))
        }
    }

    override fun close() {
    }
}

fun String.toInputStream(): InputStream {
    return AvroDeserializer::class.java.getResourceAsStream("/$this")!!
}
