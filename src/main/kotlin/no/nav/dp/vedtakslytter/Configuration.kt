package no.nav.dp.vedtakslytter

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.dp.vedtakslytter.avro.AvroDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

object Configuration {
    private val config by lazy {
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables
    }

    val consumerTopic = "aapen-arena-dagpengevedtakferdigstilt-v1"
    val producerTopic = "teamdagpenger.subsumsjonbrukt.v1"
    val commonProps: Properties by lazy {
        Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config[Key("KAFKA_BROKERS", stringType)])
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, config[Key("KAFKA_TRUSTSTORE_PATH", stringType)])
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, config[Key("KAFKA_CREDSTORE_PASSWORD", stringType)])
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, config[Key("KAFKA_KEYSTORE_PATH", stringType)])
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, config[Key("KAFKA_CREDSTORE_PASSWORD", stringType)])
        }
    }

    val consumerProps: Properties by lazy {
        val schemaRegistryUser = config[Key("KAFKA_SCHEMA_REGISTRY_USER", stringType)]
        val schemaRegistryPassword = config[Key("KAFKA_SCHEMA_REGISTRY_PASSWORD", stringType)]
        Properties().apply {
            putAll(commonProps)
            put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
            put(SchemaRegistryClientConfig.USER_INFO_CONFIG, "$schemaRegistryUser:$schemaRegistryPassword")
            put(
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                config[Key("KAFKA_SCHEMA_REGISTRY", stringType)]
            )
            put(ConsumerConfig.GROUP_ID_CONFIG, config[Key("kafka.consumer.groupid", stringType)])
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer::class.java)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        }
    }

    val producerProps: Properties by lazy {
        Properties().apply {
            putAll(commonProps)
            put(ProducerConfig.ACKS_CONFIG, "1")
            put(ProducerConfig.LINGER_MS_CONFIG, "0")
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.CLIENT_ID_CONFIG, "dp-vedtakresultat-lytter")
        }
    }
}
