package no.nav.dp.vedtakslytter

import kotlinx.coroutines.runBlocking
import no.nav.dp.health.HealthServer

fun main(args: Array<String>) {
    runBlocking {
        val config = Configuration()
        HealthServer.startServer(config.application.httpPort).start()
    }
}
