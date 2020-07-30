package no.nav.dp.vedtakslytter

import com.squareup.moshi.JsonAdapter
import io.prometheus.client.Counter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.dp.vedtakslytter.avro.AvroDeserializer
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.coroutines.CoroutineContext

object KafkaLytter : CoroutineScope {
    val logger = KotlinLogging.logger {}
    lateinit var job: Job
    lateinit var config: Configuration
    val MESSAGES_SENT = Counter.build().name("subsumsjon_brukt_sendt").help("Subsumsjoner sendt videre til Kafka")
        .labelNames("subsumsjonstype", "utfall", "status").register()
    val MESSAGES_RECEIVED = Counter.build().name("vedtakresultat_mottatt").help("Vedtakresultat mottatt").register()
    val FAILED_KAFKA_OPS =
        Counter.build().name("subsumsjon_brukt_error").help("Feil i sending av transformert melding").register()
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

    fun run() {
        launch {
            logger.info("Starter kafka consumer")
            KafkaConsumer<String, GenericRecord>(config.kafka.toConsumerProps()).use { consumer ->
                consumer.subscribe(listOf(config.kafka.topic))
                while (job.isActive) {
                    try {
                        val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                        records.asSequence().map {
                            it.key() to Vedtak.fromGenericRecord(it.value())
                        }.onEach { logger.info { it } }
                            .onEach { MESSAGES_RECEIVED.inc() }
                            .forEach { (_, v) -> handleVedtak(v) }
                    } catch (e: RetriableException) {
                        logger.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }

    fun handleVedtak(vedtak: Vedtak) {
        vedtak.minsteInntektSubsumsjonsId?.let {
            orienterOmSubsumsjon(
                SubsumsjonBrukt(
                    id = it,
                    eksternId = vedtak.vedtakId.roundedString(),
                    arenaTs = vedtak.opTs,
                    utfall = vedtak.utfallKode,
                    vedtakStatus = vedtak.vedtakStatusKode
                ),
                SubsumsjonType.MINSTEINNTEKT
            )
        }
        vedtak.periodeSubsumsjonsId?.let {
            orienterOmSubsumsjon(
                SubsumsjonBrukt(
                    eksternId = vedtak.vedtakId.roundedString(),
                    id = it,
                    arenaTs = vedtak.opTs,
                    utfall = vedtak.utfallKode,
                    vedtakStatus = vedtak.vedtakStatusKode
                ),
                SubsumsjonType.PERIODE
            )
        }
        vedtak.grunnlagSubsumsjonsId?.let {
            orienterOmSubsumsjon(
                SubsumsjonBrukt(
                    eksternId = vedtak.vedtakId.roundedString(),
                    id = it,
                    arenaTs = vedtak.opTs,
                    utfall = vedtak.utfallKode,
                    vedtakStatus = vedtak.vedtakStatusKode
                ),
                SubsumsjonType.GRUNNLAG
            )
        }
        vedtak.satsSubsumsjonsId?.let {
            orienterOmSubsumsjon(
                SubsumsjonBrukt(
                    eksternId = vedtak.vedtakId.toString(),
                    id = it,
                    arenaTs = vedtak.opTs,
                    utfall = vedtak.utfallKode,
                    vedtakStatus = vedtak.vedtakStatusKode
                ),
                SubsumsjonType.SATS
            )
        }
    }

    private fun orienterOmSubsumsjon(subsumsjonBrukt: SubsumsjonBrukt, subsumsjonType: SubsumsjonType) {
        KafkaProducer<String, String>(config.kafka.toProducerProps()).use { p ->
            p.send(
                ProducerRecord(
                    config.kafka.subsumsjonBruktTopic,
                    subsumsjonBrukt.id,
                    subsumsjonAdapter.toJson(subsumsjonBrukt)
                )
            ) { d, e ->
                if (d != null) {
                    val utfall = subsumsjonBrukt.utfall ?: "ukjent"
                    val vedtakstatus = subsumsjonBrukt.vedtakStatus ?: "ukjent"
                    MESSAGES_SENT.labels(subsumsjonType.toString().toLowerCase(), utfall, vedtakstatus).inc()
                    logger.debug { "Sendte bekreftelse på subsumsjon brukt [$subsumsjonBrukt] - offset ${d.offset()}" }
                } else if (e != null) {
                    FAILED_KAFKA_OPS.inc()
                    logger.error("Kunne ikke sende bekreftelse på subsumsjon", e)
                }
            }
        }
    }
}
enum class SubsumsjonType {
    SATS, GRUNNLAG, PERIODE, MINSTEINNTEKT
}
val subsumsjonAdapter: JsonAdapter<SubsumsjonBrukt> = moshiInstance.adapter(SubsumsjonBrukt::class.java)

fun Double.roundedString(): String {
    return if (this.toLong().toDouble().equals(this)) {
        String.format("%.0f", this)
    } else {
        this.toString()
    }
}

data class SubsumsjonBrukt(
    val eksternId: String,
    val id: String,
    val arenaTs: ZonedDateTime,
    val ts: Long = Instant.now().toEpochMilli(),
    val utfall: String?,
    val vedtakStatus: String?
)

data class Vedtak(
    val table: String,
    val opType: String,
    val opTs: ZonedDateTime,
    val currentTs: ZonedDateTime,
    val pos: String,
    val primaryKeys: List<String> = emptyList(),
    val tokens: Map<String, String> = emptyMap(),
    val vedtakId: Double,
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
            .set("op_ts", opTs.format(arenaOpTsFormat))
            .set("current_ts", currentTs.format(arenaCurrentTsFormat))
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

    @Suppress("UNCHECKED_CAST")
    companion object {
        private val arenaOpTsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")
        private val arenaCurrentTsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]")
        private val oslo: ZoneId = ZoneId.of("Europe/Oslo")
        fun fromGenericRecord(record: GenericRecord): Vedtak {
            return Vedtak(
                table = record.get("table").toString(),
                opType = record.get("op_type").toString(),
                opTs = LocalDateTime.parse(record.get("op_ts").toString(), arenaOpTsFormat).atZone(oslo),
                currentTs = LocalDateTime.parse(record.get("current_ts").toString(), arenaCurrentTsFormat).atZone(oslo),
                pos = record.get("pos").toString(),
                primaryKeys = record.get("primary_keys") as List<String>,
                tokens = record.get("tokens") as Map<String, String>,
                vedtakId = record.get("VEDTAK_ID") as Double,
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
