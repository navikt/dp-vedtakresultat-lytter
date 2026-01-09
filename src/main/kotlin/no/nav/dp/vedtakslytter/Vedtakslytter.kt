package no.nav.dp.vedtakslytter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.naisful.naisApp
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.dp.vedtakslytter.Configuration.httpPort
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

fun main() {
    runBlocking {
        KafkaLytter.apply {
            create()
            run()
        }
        launch(IO) {
            while (true) {
                if (KafkaLytter.isRunning()) {
                    logger.trace("Still running")
                }
                delay(TimeUnit.SECONDS.toMillis(60))
            }
        }
        val objectMapper: ObjectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val app =
            naisApp(
                meterRegistry =
                    PrometheusMeterRegistry(
                        PrometheusConfig.DEFAULT,
                        PrometheusRegistry.defaultRegistry,
                        Clock.SYSTEM,
                    ),
                objectMapper = objectMapper,
                applicationLogger = LoggerFactory.getLogger("ApplicationLogger"),
                callLogger = LoggerFactory.getLogger("CallLogger"),
                aliveCheck = { KafkaLytter.isRunning() },
                port = httpPort,
            ) {}
        app.start(wait = true)
    }
}

val logger: Logger = LoggerFactory.getLogger("no.nav.dp.vedtakslytter.VedtaksLytter")
