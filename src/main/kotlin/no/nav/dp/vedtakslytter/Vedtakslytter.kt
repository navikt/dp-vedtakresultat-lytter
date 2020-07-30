package no.nav.dp.vedtakslytter

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.dp.health.HealthServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    runBlocking {
        val config = Configuration()

        HealthServer.startServer(config.application.httpPort).start(wait = false)
        KafkaLytter.apply {
            create(config)
            run()
        }
        GlobalScope.launch {
            while (true) {
                if (KafkaLytter.isRunning()) {
                    logger.trace("Still running")
                }
                delay(TimeUnit.SECONDS.toMillis(60))
            }
        }
    }
}

val logger: Logger = LoggerFactory.getLogger("no.nav.dp.vedtakslytter.VedtaksLytter")
