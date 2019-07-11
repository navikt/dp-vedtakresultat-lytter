package no.nav.dp.vedtakslytter

import com.natpryce.konfig.*
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier
import no.nav.dp.vedtakslytter.avro.AvroDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.io.File
import java.util.*

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "application.httpPort" to "8099",
        "kafka.bootstrapServer" to "localhost:9092",
        "kafka.schemaRegistryServer" to "http://localhost:8081",
        "kafka.topic" to "privat-arena-dagpengevedtak-ferdigstilt",
        "username" to "srvdp-vedtakresultat",
        "password" to "ikkenoe",
        "kafka.groupId" to "srvdp-vedtakresultat-lytter",
        "kafka.subsumsjon.topic" to "private-dagpenger-subsumsjon-brukt",
        "regel.api.url" to "http://dp-regel-api.nais.preprod.local",
        "oidc.sts.issuerurl" to "http://localhost",
        "srvdp.vedtakresultat.lytter.username" to "srvdp-vedtakresultat",
        "srvdp.vedtakresultat.lytter.password" to "srvdp-passord",
        "auth.regelapi.secret" to "secret",
        "auth.regelapi.key" to "key"
    )
)

private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",
        "application.httpPort" to "8099",
        "kafka.bootstrapServer" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "kafka.schemaRegistryServer" to "https://kafka-schema-registry.nais.preprod.local",
        "kafka.topic" to "privat-arena-dagpengevedtak-ferdigstilt",
        "username" to "srvdp-vedtakresultat",
        "password" to "ikkenoe",
        "kafka.groupId" to "vedtakresultat-lytter",
        "kafka.subsumsjon.topic" to "private-dagpenger-subsumsjon-brukt",
        "regel.api.url" to "http://dp-regel-api",
        "oidc.sts.issuerurl" to "http://localhost",
        "srvdp.vedtakresultat.lytter.username" to "srvdp-vedtakresultat",
        "srvdp.vedtakresultat.lytter.password" to "srvdp-passord",
        "kafka.groupId" to "srvdp-vedtakresultat-lytter",
        "auth.regelapi.secret" to "secret",
        "auth.regelapi.key" to "key"
    )
)

private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD",
        "application.httpPort" to "8099",
        "kafka.bootstrapServer" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00149.adeo.no:8443",
        "kafka.schemaRegistryServer" to "http://kafka-schema-registry.tpa:8081",
        "kafka.topic" to "privat-arena-dagpengevedtak-ferdigstilt",
        "username" to "srvdp-vedtakresultat",
        "password" to "ikkenoe",
        "kafka.groupId" to "srvdp-vedtakresultat-lytter",
        "kafka.subsumsjon.topic" to "private-dagpenger-subsumsjon-brukt",
        "regel.api.url" to "http://dp-regel-api",
        "srvdp.vedtakresultat.lytter.username" to "srvdp-vedtakresultat",
        "srvdp.vedtakresultat.lytter.password" to "srvdp-passord",
        "auth.regelapi.secret" to "secret",
        "auth.regelapi.key" to "key"
    )
)

data class Application(
    val httpPort: Int = config()[Key("application.httpPort", intType)],
    val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
    val username: String = config()[Key("srvdp.vedtakresultat.lytter.username", stringType)],
    val password: String = config()[Key("srvdp.vedtakresultat.lytter.password", stringType)]
)

data class Kafka(
    val bootstrapServer: String = config()[Key("kafka.bootstrapServer", stringType)],
    val schemaRegistryServer: String = config()[Key("kafka.schemaRegistryServer", stringType)],
    val topic: String = config()[Key("kafka.topic", stringType)],
    val username: String = config()[Key("username", stringType)],
    val password: String = config()[Key("password", stringType)],
    val groupId: String = config()[Key("kafka.groupId", stringType)],
    val subsumsjonBruktTopic: String = config()[Key("kafka.subsumsjon.topic", stringType)]
) {

    fun toConsumerProps(): Properties {
        return Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
            put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryServer)
            put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer::class.java)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
            putAll(credentials())
        }
    }

    fun toProducerProps(): Properties {
        return Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.ACKS_CONFIG, "1")
            put(ProducerConfig.CLIENT_ID_CONFIG, "dp-vedtakresultat-lytter")
            putAll(credentials())
        }
    }

    fun credentials(): Properties {
        return Properties().apply {
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(
                SaslConfigs.SASL_JAAS_CONFIG,
                """org.apache.kafka.common.security.plain.PlainLoginModule required username="$username" password="$password";"""
            )
            System.getenv("NAV_TRUSTSTORE_PATH")?.let {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("NAV_TRUSTSTORE_PASSWORD"))
            }
        }
    }
}

enum class Profile {
    LOCAL, DEV, PROD
}

data class Configuration(
    val application: Application = Application(),
    val kafka: Kafka = Kafka(),
    val regelApiUrl: String = config()[Key("regel.api.url", stringType)],
    val auth: Auth = Auth()
)

class Auth(
    regelApiSecret: String = config()[Key("auth.regelapi.secret", stringType)],
    regelApiKeyPlain: String = config()[Key("auth.regelapi.key", stringType)]
) {
    val regelApiKey = ApiKeyVerifier(regelApiSecret).generate(regelApiKeyPlain)
}

fun getEnvOrProp(propName: String): String? {
    return System.getenv(propName) ?: System.getProperty(propName)
}

private fun config() = when (getEnvOrProp("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
}