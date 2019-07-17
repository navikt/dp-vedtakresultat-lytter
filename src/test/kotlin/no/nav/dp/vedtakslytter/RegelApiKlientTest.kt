package no.nav.dp.vedtakslytter

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class RegelApiKlientTest {
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }
    @Test
    fun `Sets correct headers on request`() {
        val exampleApiKey = "abc123"
        val eksempelSubsumsjon = SubsumsjonBrukt(
            id = "123",
            eksternId = "789",
            arenaTs = LocalDateTime.now().minusHours(6).toInstant(ZoneOffset.UTC).toString()
        )
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("//subsumsjonbrukt"))
                .withHeader("Content-Type", WireMock.equalTo("application/json"))
                .withHeader("X-API-KEY", WireMock.equalTo(exampleApiKey))
                .willReturn(WireMock.aResponse().withStatus(200))
        )
        val regelApiClient = RegelApiKlient(regelApiBaseUrl = server.url(""), apiKey = exampleApiKey)
        val r = regelApiClient.orienterOmSubsumsjon(eksempelSubsumsjon)
        WireMock.verify(
            WireMock.postRequestedFor(
                WireMock.urlEqualTo("//subsumsjonbrukt"))
                        .withHeader("Content-Type", WireMock.equalTo("application/json"))
                .withRequestBody(WireMock.equalToJson(subsumsjonAdapter.toJson(eksempelSubsumsjon)))
        )
        assertEquals(200, r)
    }

    @Test
    fun `Returns http error code if request fails`() {
        val exampleApiKey = "abc123"
        val eksempelSubsumsjon = SubsumsjonBrukt(
            id = "123",
            eksternId = "789",
            arenaTs = LocalDateTime.now().minusHours(6).toInstant(ZoneOffset.UTC).toString()
        )
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("//subsumsjonbrukt"))
                .withHeader("Content-Type", WireMock.equalTo("application/json"))
                .withHeader("X-API-KEY", WireMock.equalTo(exampleApiKey))
                .willReturn(WireMock.aResponse().withStatus(500))
        )
        val regelApiClient = RegelApiKlient(regelApiBaseUrl = server.url(""), apiKey = exampleApiKey)
        val r = regelApiClient.orienterOmSubsumsjon(eksempelSubsumsjon)
        WireMock.verify(
            WireMock.postRequestedFor(
                WireMock.urlEqualTo("//subsumsjonbrukt"))
                .withHeader("Content-Type", WireMock.equalTo("application/json"))
                .withRequestBody(WireMock.equalToJson(subsumsjonAdapter.toJson(eksempelSubsumsjon)))
        )
        assertEquals(500, r)
    }
}