package be.endevops

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        logger = this@configureMonitoring.log
        filter { call ->
            call.request
                .path()
                .startsWith("/")
        }
        disableForStaticContent()
    }
}
