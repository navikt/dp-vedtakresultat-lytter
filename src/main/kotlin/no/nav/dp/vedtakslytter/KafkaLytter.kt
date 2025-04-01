package no.nav.dp.vedtakslytter

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.prometheus.metrics.core.metrics.Counter
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
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

object KafkaLytter : CoroutineScope {
    val logger = KotlinLogging.logger {}
    lateinit var job: Job
    val MESSAGES_SENT =
        Counter
            .builder()
            .name("subsumsjon_brukt_sendt")
            .help("Subsumsjoner sendt videre til Kafka")
            .labelNames("subsumsjonstype", "utfall", "status")
            .register()
    val MESSAGES_RECEIVED =
        Counter
            .builder()
            .name("vedtakresultat_mottatt")
            .help("Vedtakresultat mottatt")
            .register()
    val FAILED_KAFKA_OPS =
        Counter
            .builder()
            .name("subsumsjon_brukt_error")
            .help("Feil i sending av transformert melding")
            .register()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    val kafkaProducer by lazy { KafkaProducer<String, String>(Configuration.producerProps) }
    val vedtakHandler by lazy { VedtakHandler(kafkaProducer, Configuration.producerTopic) }

    init {
        Runtime.getRuntime().addShutdownHook(Thread(::cancel))
    }

    fun cancel() {
        kafkaProducer.close()
        job.cancel()
    }

    fun isRunning(): Boolean {
        logger.trace { "Asked if running" }
        return job.isActive && producerIsAlive()
    }

    private fun producerIsAlive(): Boolean =
        try {
            kafkaProducer.partitionsFor(Configuration.producerTopic)
            true
        } catch (e: Exception) {
            logger.error(e) { "Producer er ikke alive" }
            false
        }

    fun create() {
        this.job = Job()
    }

    fun run() {
        launch {
            logger.info("Starter kafka consumer")
            KafkaConsumer<String, GenericRecord>(Configuration.consumerProps).use { consumer ->
                consumer.subscribe(listOf(Configuration.consumerTopic))
                while (job.isActive) {
                    try {
                        val records = consumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                        records
                            .asSequence()
                            .map {
                                it.key() to Vedtak.fromGenericRecord(it.value())
                            }.onEach { logger.info { it } }
                            .onEach { MESSAGES_RECEIVED.inc() }
                            .forEach { (_, v) -> vedtakHandler.handleVedtak(v) }

                        consumer.commitSync()
                    } catch (e: RetriableException) {
                        logger.warn("Had a retriable exception, retrying", e)
                    }
                }
            }
        }
    }
}

class VedtakHandler(
    private val kafkaProducer: Producer<String, String>,
    private val topic: String,
) {
    fun handleVedtak(vedtak: Vedtak) {
        vedtak.minsteInntektSubsumsjonsId?.let {
            orienterOmSubsumsjon(
                SubsumsjonBrukt(
                    id = it,
                    eksternId = vedtak.vedtakId.roundedString(),
                    arenaTs = vedtak.opTs,
                    utfall = vedtak.utfallKode,
                    vedtakStatus = vedtak.vedtakStatusKode,
                ),
                SubsumsjonType.MINSTEINNTEKT,
            )
        }
        vedtak.periodeSubsumsjonsId?.let {
            orienterOmSubsumsjon(
                SubsumsjonBrukt(
                    eksternId = vedtak.vedtakId.roundedString(),
                    id = it,
                    arenaTs = vedtak.opTs,
                    utfall = vedtak.utfallKode,
                    vedtakStatus = vedtak.vedtakStatusKode,
                ),
                SubsumsjonType.PERIODE,
            )
        }
        vedtak.grunnlagSubsumsjonsId?.let {
            orienterOmSubsumsjon(
                SubsumsjonBrukt(
                    eksternId = vedtak.vedtakId.roundedString(),
                    id = it,
                    arenaTs = vedtak.opTs,
                    utfall = vedtak.utfallKode,
                    vedtakStatus = vedtak.vedtakStatusKode,
                ),
                SubsumsjonType.GRUNNLAG,
            )
        }
        vedtak.satsSubsumsjonsId?.let {
            orienterOmSubsumsjon(
                SubsumsjonBrukt(
                    eksternId = vedtak.vedtakId.toString(),
                    id = it,
                    arenaTs = vedtak.opTs,
                    utfall = vedtak.utfallKode,
                    vedtakStatus = vedtak.vedtakStatusKode,
                ),
                SubsumsjonType.SATS,
            )
        }
    }

    private fun orienterOmSubsumsjon(
        subsumsjonBrukt: SubsumsjonBrukt,
        subsumsjonType: SubsumsjonType,
    ) {
        try {
            val metadata =
                kafkaProducer
                    .send(
                        ProducerRecord(
                            topic,
                            subsumsjonBrukt.id,
                            subsumsjonAdapter.writeValueAsString(subsumsjonBrukt),
                        ),
                    ).get(500, TimeUnit.MILLISECONDS)

            val utfall = subsumsjonBrukt.utfall ?: "ukjent"
            val vedtakstatus = subsumsjonBrukt.vedtakStatus ?: "ukjent"
            KafkaLytter.MESSAGES_SENT.labelValues(subsumsjonType.toString().lowercase(), utfall, vedtakstatus).inc()
            KafkaLytter.logger.debug { "Sendte bekreftelse på subsumsjon brukt [$subsumsjonBrukt] - offset ${metadata.offset()}" }
        } catch (e: Exception) {
            KafkaLytter.FAILED_KAFKA_OPS.inc()
            KafkaLytter.logger.error("Kunne ikke sende bekreftelse på subsumsjon", e)
            throw e
        }
    }
}

enum class SubsumsjonType {
    SATS,
    GRUNNLAG,
    PERIODE,
    MINSTEINNTEKT,
}

val subsumsjonAdapter =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

fun Double.roundedString(): String =
    if (this.toLong().toDouble().equals(this)) {
        String.format("%.0f", this)
    } else {
        this.toString()
    }

data class SubsumsjonBrukt(
    val eksternId: String,
    val id: String,
    val arenaTs: ZonedDateTime,
    val ts: Long = Instant.now().toEpochMilli(),
    val utfall: String?,
    val vedtakStatus: String?,
)

data class Vedtak(
    val table: String,
    val opType: String,
    val opTs: ZonedDateTime,
    val currentTs: ZonedDateTime,
    val pos: String,
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
    val modDato: String? = null,
) {
    fun toGenericRecordV1(): GenericRecord =
        GenericRecordBuilder(AvroDeserializer.dagpengeVedtakSchemaV1)
            .set("table", table)
            .set("op_type", opType)
            .set("op_ts", opTs.format(arenaOpTsFormat))
            .set("current_ts", currentTs.format(arenaCurrentTsFormat))
            .set("pos", pos)
            .set("primary_keys", emptyList<String>())
            .set("tokens", emptyMap<String, String>())
            .set("VEDTAK_ID", vedtakId)
            .set("VEDTAKTYPEKODE", vedtakTypeKode)
            .set("VEDTAKSTATUSKODE", vedtakStatusKode)
            .set("UTFALLKODE", utfallKode)
            .set("MINSTEINNTEKT_SUBSUMSJONSID", minsteInntektSubsumsjonsId)
            .set("PERIODE_SUBSUMSJONSID", periodeSubsumsjonsId)
            .set("GRUNNLAG_SUBSUMSJONSID", grunnlagSubsumsjonsId)
            .set("SATS_SUBSUMSJONSID", satsSubsumsjonsId)
            .set("REG_USER", regUser)
            .set("REG_DATO", regDato)
            .set("MOD_USER", modUser)
            .set("MOD_DATO", modDato)
            .build()

    fun toGenericRecordV2(): GenericRecord =
        GenericRecordBuilder(AvroDeserializer.dagpengeVedtakSchemaV2)
            .set("table", table)
            .set("op_type", opType)
            .set("op_ts", opTs.format(arenaOpTsFormat))
            .set("current_ts", currentTs.format(arenaCurrentTsFormat))
            .set("pos", pos)
            .set("VEDTAK_ID", vedtakId)
            .set("VEDTAKTYPEKODE", vedtakTypeKode)
            .set("VEDTAKSTATUSKODE", vedtakStatusKode)
            .set("UTFALLKODE", utfallKode)
            .set("MINSTEINNTEKT_SUBSUMSJONSID", minsteInntektSubsumsjonsId)
            .set("PERIODE_SUBSUMSJONSID", periodeSubsumsjonsId)
            .set("GRUNNLAG_SUBSUMSJONSID", grunnlagSubsumsjonsId)
            .set("SATS_SUBSUMSJONSID", satsSubsumsjonsId)
            .set("REG_USER", regUser)
            .set("REG_DATO", regDato)
            .set("MOD_USER", modUser)
            .set("MOD_DATO", modDato)
            .build()

    @Suppress("UNCHECKED_CAST")
    companion object {
        private val arenaOpTsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")
        private val arenaCurrentTsFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]")
        private val oslo: ZoneId = ZoneId.of("Europe/Oslo")

        fun fromGenericRecord(record: GenericRecord): Vedtak =
            Vedtak(
                table = record.get("table").toString(),
                opType = record.get("op_type").toString(),
                opTs = LocalDateTime.parse(record.get("op_ts").toString(), arenaOpTsFormat).atZone(oslo),
                currentTs = readCurrentTs(record),
                pos = record.get("pos").toString(),
                vedtakId = record.get("VEDTAK_ID") as Double,
                vedtakTypeKode = record.get("VEDTAKTYPEKODE")?.toString(),
                vedtakStatusKode = record.get("VEDTAKSTATUSKODE")?.toString(),
                utfallKode = record.get("UTFALLKODE")?.toString(),
                minsteInntektSubsumsjonsId = record.get("MINSTEINNTEKT_SUBSUMSJONSID")?.toString(),
                periodeSubsumsjonsId = record.get("PERIODE_SUBSUMSJONSID")?.toString(),
                satsSubsumsjonsId = record.get("SATS_SUBSUMSJONSID")?.toString(),
                grunnlagSubsumsjonsId = record.get("GRUNNLAG_SUBSUMSJONSID")?.toString(),
                regUser = record.get("REG_USER")?.toString(),
                regDato = record.get("REG_DATO")?.toString(),
                modUser = record.get("MOD_USER")?.toString(),
                modDato = record.get("MOD_DATO")?.toString(),
            )

        private fun readCurrentTs(record: GenericRecord): ZonedDateTime =
            with(record.get("current_ts").toString()) {
                try {
                    LocalDateTime.parse(this, arenaCurrentTsFormat).atZone(oslo)
                } catch (e: DateTimeParseException) {
                    LocalDateTime.parse(this, arenaOpTsFormat).atZone(oslo)
                }
            }
    }
}
