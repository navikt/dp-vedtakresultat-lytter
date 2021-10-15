package no.nav.dp.vedtakslytter.avro

import org.apache.avro.Schema
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DecoderFactory
import org.apache.kafka.common.serialization.Deserializer
import java.io.InputStream

class AvroDeserializer : Deserializer<GenericRecord> {
    companion object {
        val schema = Schema.Parser().parse("GRENSESNITT.FERDIGSTILTE_DAGPENGEVEDTAK.avsc".toInputStream())
    }
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
    }

    override fun deserialize(topic: String, data: ByteArray): GenericRecord {
        val reader = GenericDatumReader<GenericRecord>(schema)
        return reader.read(null, DecoderFactory.get().binaryDecoder(data, null))
    }

    override fun close() {
    }
}

fun String.toInputStream(): InputStream {
    return AvroDeserializer::class.java.getResourceAsStream("/$this")!!
}
