package com.cbgm.securechat.server.observability

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.UUID

const val REQUEST_ID_MDC_KEY: String = "request_id"

private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9._-]{1,128}")

fun Application.installServerObservability(
    serviceName: String,
    readinessCheck: suspend () -> Boolean = { true }
) {
    require(serviceName.isNotBlank()) {
        "Observability service name must not be blank"
    }

    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    prometheusRegistry.config().commonTags("service", serviceName)

    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify(REQUEST_ID_PATTERN::matches)
    }
    install(CallLogging) {
        callIdMdc(REQUEST_ID_MDC_KEY)
    }
    install(MicrometerMetrics) {
        registry = prometheusRegistry
    }

    routing {
        get("/health/live") {
            call.respondText("ok service=$serviceName")
        }

        get("/health/ready") {
            val ready = runCatching { readinessCheck() }.getOrDefault(false)
            call.respondText(
                text = if (ready) "ready service=$serviceName" else "not ready service=$serviceName",
                status = if (ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            )
        }

        get("/metrics") {
            call.respondText(
                text = prometheusRegistry.scrape(),
                contentType = ContentType.Text.Plain
            )
        }
    }
}
