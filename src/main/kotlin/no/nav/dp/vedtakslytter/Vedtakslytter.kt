package no.nav.dp.vedtakslytter

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        KtorServer.startServer(port = 8099).start(wait = true)
    }
}

val logger: Logger = LoggerFactory.getLogger("no.nav.dp.vedtakslytter.VedtaksLytter")
