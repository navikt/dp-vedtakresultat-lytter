package no.nav.dp.vedtakslytter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.dp.vedtakslytter.avro.AvroDeserializer
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.coroutines.CoroutineContext

object KafkaLytter : CoroutineScope {
    val logger = KotlinLogging.logger {}
    lateinit var job: Job
    lateinit var config: Configuration
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    fun cancel() {
        job.cancel()
    }

    fun isRunning(): Boolean {
        logger.trace { "Asked if running" }
        return job.isActive
    }

    fun create(config: Configuration) {
        this.job = Job()
        this.config = config
    }

    suspend fun run() {
        launch {
            logger.info { config.kafka.username }
            KafkaConsumer<String, GenericRecord>(config.kafka.toConsumerProps()).use { consumer ->
                consumer.subscribe(listOf(config.kafka.topic))
                while (job.isActive) {
                    try {
                        val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                        records.asSequence().map {
                            it.key() to Vedtak.fromGenericRecord(it.value())
                        }.forEach { logger.info { it } }
                    } catch (error: OutOfMemoryError) {
                        logger.error("Out of memory while polling kafka", error)
                        job.cancel()
                    } catch (e: RetriableException) {
                        logger.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }
}

data class Vedtak(
    val table: String,
    val opType: String,
    val opTs: String,
    val currentTs: String,
    val pos: String,
    val primaryKeys: List<String> = emptyList(),
    val tokens: Map<String, String> = emptyMap(),
    val vedtakId: Double? = null,
    val vedtakTypeKode: String? = null,
    val vedtakStatusKode: String? = null,
    val utfallKode: String? = null,
    val minsteInntektSubsumsjonsId: String? = null,
    val periodeSubsumsjonsId: String? = null,
    val grunnlagSubsumsjonsId: String? = null,
    val satsSubsumsjonsId: String? = null,
    val regUser: String? = null,
    val regDato: String? = null,
    val modUser: String? = null,
    val modDato: String? = null
) {
    fun toGenericRecord(): GenericRecord {
        return GenericRecordBuilder(AvroDeserializer.schema)
            .set("table", table)
            .set("op_type", opType)
            .set("op_ts", opTs)
            .set("current_ts", currentTs)
            .set("primary_keys", primaryKeys)
            .set("pos", pos)
            .set("tokens", tokens)
            .set("VEDTAK_ID", vedtakId)
            .set("VEDTAKTYPEKODE", vedtakTypeKode)
            .set("VEDTAKSTATUSKODE", vedtakStatusKode)
            .set("UTFALLKODE", utfallKode)
            .set("MINSTEINNTEKT_SUBSUMSJONSID", minsteInntektSubsumsjonsId)
            .set("PERIODE_SUBSUMSJONSID", periodeSubsumsjonsId)
            .set("GRUNNLAG_SUBSUMSJONSID", grunnlagSubsumsjonsId)
            .set("REG_USER", regUser)
            .set("REG_DATO", regDato)
            .set("MOD_USER", modUser)
            .set("MOD_DATO", modDato).build()
    }

    companion object {
        fun fromGenericRecord(record: GenericRecord): Vedtak {
            return Vedtak(
                table = record.get("table").toString(),
                opType = record.get("op_type").toString(),
                opTs = record.get("op_ts").toString(),
                currentTs = record.get("current_ts").toString(),
                pos = record.get("pos").toString(),
                primaryKeys = record.get("primary_keys") as List<String>,
                tokens = record.get("tokens") as Map<String, String>,
                vedtakId = record.get("VEDTAK_ID") as Double?,
                vedtakTypeKode = record.get("VEDTAKTYPEKODE")?.toString(),
                vedtakStatusKode = record.get("VEDTAKSTATUSKODE")?.toString(),
                utfallKode = record.get("UTFALLKODE")?.toString(),
                minsteInntektSubsumsjonsId = record.get("MINSTEINNTEKT_SUBSUMSJONSID")?.toString(),
                periodeSubsumsjonsId = record.get("PERIODE_SUBSUMSJONSID")?.toString(),
                satsSubsumsjonsId = record.get("GRUNNLAG_SUBSUMSJONSID")?.toString(),
                regUser = record.get("REG_USER")?.toString(),
                regDato = record.get("REG_DATO")?.toString(),
                modUser = record.get("MOD_USER")?.toString(),
                modDato = record.get("MOD_DATO")?.toString()
            )
        }
    }

    override fun toString(): String {
        return "Vedtak(table='$table', opType='$opType', opTs='$opTs', currentTs='$currentTs', pos='$pos', primaryKeys='$primaryKeys', tokens=$tokens, vedtakId=$vedtakId, vedtakTypeKode=$vedtakTypeKode, vedtakStatusKode=$vedtakStatusKode, utfallKode=$utfallKode, minsteInntektSubsumsjonsId=$minsteInntektSubsumsjonsId, periodeSubsumsjonsId=$periodeSubsumsjonsId, grunnlagSubsumsjonsId=$grunnlagSubsumsjonsId, satsSubsumsjonsId=$satsSubsumsjonsId)"
    }
}