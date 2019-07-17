package no.nav.dp.vedtakslytter

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import de.huxhorn.sulky.ulid.ULID
import mu.KotlinLogging

val ulid = ULID()
val subsumsjonAdapter = moshiInstance.adapter(SubsumsjonBrukt::class.java)
val LOGGER = KotlinLogging.logger {}
class RegelApiKlient(private val regelApiBaseUrl: String, val apiKey: String) {
    fun orienterOmSubsumsjon(subsumsjonBrukt: SubsumsjonBrukt): Int {
        val jsonBody = subsumsjonAdapter.toJson(subsumsjonBrukt)
        val subsumsjonsUrl = "$regelApiBaseUrl/subsumsjonbrukt"
        val (_, _, result) = with(subsumsjonsUrl.httpPost()) {
            header("X-API-KEY", apiKey)
            header("Nav-Consumer-Id", "dp-vedtakresultat-lytter")
            header("Nav-Call-Id", ulid.nextULID())
            body(jsonBody)
            responseObject(moshiDeserializerOf(String::class.java))
        }
        return result.fold(
            success = { 200 },
            failure = { e ->
                LOGGER.error("Klarte ikke å orientere om subsumsjon $subsumsjonBrukt", e)
                e.response.statusCode
            }
        )
    }
}