package no.nav.dp.vedtakslytter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Arrays
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

    fun create() {
        this.job = Job()
        this.config = Configuration()
    }

    suspend fun run() {
        launch {
            KafkaConsumer<String, GenericRecord>(config.kafka.toConsumerProps()).use { consumer ->
                consumer.subscribe(listOf(config.kafka.topic))
                while (job.isActive) {
                    try {
                        val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                        records.map {
                            val record = it.value()
                            Vedtak(
                                table = record.get("table") as String,
                                opType = record.get("op_type") as String,
                                opTs = record.get("op_ts") as String,
                                currentTs = record.get("current_ts") as String,
                                pos = record.get("pos") as String,
                                primaryKeys = record.get("primary_keys") as Array<String>,
                                tokens = record.get("tokens") as Map<String, String>,
                                vedtakId = record.get("VEDTAK_ID") as Double?,
                                vedtakTypeKode = record.get("VEDTAKTYPEKODE") as String?,
                                vedtakStatusKode = record.get("VEDTAKSTATUSKODE") as String?,
                                utfallKode = record.get("UTFALLKODE") as String?,
                                minsteInntektSubsumsjonsId = record.get("MINSTEINNTEKT_SUBSUMSJONSID") as String?,
                                periodeSubsumsjonsId = record.get("PERIODE_SUBSUMSJONSID") as String?,
                                satsSubsumsjonsId = record.get("GRUNNLAG_SUBSUMSJONSID") as String?,
                                regUser = record.get("REG_USER") as String?,
                                regDato = record.get("REG_DATO") as String?,
                                modUser = record.get("MOD_USER") as String?,
                                modDato = record.get("MOD_DATO") as String?
                            )
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
    val primaryKeys: Array<String>,
    val tokens: Map<String, String>,
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vedtak

        if (table != other.table) return false
        if (opType != other.opType) return false
        if (opTs != other.opTs) return false
        if (currentTs != other.currentTs) return false
        if (pos != other.pos) return false
        if (!primaryKeys.contentEquals(other.primaryKeys)) return false
        if (tokens != other.tokens) return false
        if (vedtakId != other.vedtakId) return false
        if (vedtakTypeKode != other.vedtakTypeKode) return false
        if (vedtakStatusKode != other.vedtakStatusKode) return false
        if (utfallKode != other.utfallKode) return false
        if (minsteInntektSubsumsjonsId != other.minsteInntektSubsumsjonsId) return false
        if (periodeSubsumsjonsId != other.periodeSubsumsjonsId) return false
        if (grunnlagSubsumsjonsId != other.grunnlagSubsumsjonsId) return false
        if (satsSubsumsjonsId != other.satsSubsumsjonsId) return false
        if (regUser != other.regUser) return false
        if (regDato != other.regDato) return false
        if (modUser != other.modUser) return false
        if (modDato != other.modDato) return false

        return true
    }

    override fun hashCode(): Int {
        var result = table.hashCode()
        result = 31 * result + opType.hashCode()
        result = 31 * result + opTs.hashCode()
        result = 31 * result + currentTs.hashCode()
        result = 31 * result + pos.hashCode()
        result = 31 * result + primaryKeys.contentHashCode()
        result = 31 * result + tokens.hashCode()
        result = 31 * result + (vedtakId?.hashCode() ?: 0)
        result = 31 * result + (vedtakTypeKode?.hashCode() ?: 0)
        result = 31 * result + (vedtakStatusKode?.hashCode() ?: 0)
        result = 31 * result + (utfallKode?.hashCode() ?: 0)
        result = 31 * result + (minsteInntektSubsumsjonsId?.hashCode() ?: 0)
        result = 31 * result + (periodeSubsumsjonsId?.hashCode() ?: 0)
        result = 31 * result + (grunnlagSubsumsjonsId?.hashCode() ?: 0)
        result = 31 * result + (satsSubsumsjonsId?.hashCode() ?: 0)
        result = 31 * result + (regUser?.hashCode() ?: 0)
        result = 31 * result + (regDato?.hashCode() ?: 0)
        result = 31 * result + (modUser?.hashCode() ?: 0)
        result = 31 * result + (modDato?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Vedtak(table='$table', opType='$opType', opTs='$opTs', currentTs='$currentTs', pos='$pos', primaryKeys=${Arrays.toString(
            primaryKeys
        )}, tokens=$tokens, vedtakId=$vedtakId, vedtakTypeKode=$vedtakTypeKode, vedtakStatusKode=$vedtakStatusKode, utfallKode=$utfallKode, minsteInntektSubsumsjonsId=$minsteInntektSubsumsjonsId, periodeSubsumsjonsId=$periodeSubsumsjonsId, grunnlagSubsumsjonsId=$grunnlagSubsumsjonsId, satsSubsumsjonsId=$satsSubsumsjonsId, regUser=$regUser, modUser=$modUser)"
    }
}