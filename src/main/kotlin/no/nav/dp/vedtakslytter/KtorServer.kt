package no.nav.dp.vedtakslytter

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports

object KtorServer {
    suspend fun startServer(port: Int): CIOApplicationEngine {
        DefaultExports.initialize()
        return embeddedServer(CIO, port = port) {
            install(DefaultHeaders)
            install(MicrometerMetrics) {
                registry =
                    PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)
            }
            routing {
                get("/metrics") {
                    val names = call.request.queryParameters.getAll("name")?.toSet() ?: emptySet()
                    call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004), HttpStatusCode.OK) {
                        TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
                    }
                }
                get("/isAlive") {
                    if (KafkaLytter.isRunning()) {
                        call.respondText(text = "ALIVE", contentType = ContentType.Text.Plain)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }

                get("/isReady") {
                    call.respondText(text = "READY", contentType = ContentType.Text.Plain)
                }
            }
        }
    }
}
