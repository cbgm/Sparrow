package com.cbgm.sparrow.server.exchangerates

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException

/** Euro reference rates in the format of the Frankfurter API, which the app reads. */
const val DEFAULT_EXCHANGE_RATES_URL = "https://api.frankfurter.dev/v2/rates?base=EUR"

private const val DEFAULT_CACHE_TTL_MILLISECONDS = 24L * 60L * 60L * 1_000L

/**
 * Serves the exchange rates for group expenses, so the upstream service sees this node
 * instead of every app. The response is passed through unchanged and cached for all clients.
 */
fun Application.installExchangeRateRoutes(
    upstreamUrl: String = DEFAULT_EXCHANGE_RATES_URL,
    cacheTtlMilliseconds: Long = DEFAULT_CACHE_TTL_MILLISECONDS
) {
    val cache = ExchangeRateCache(upstreamUrl, cacheTtlMilliseconds)

    monitor.subscribe(ApplicationStopped) {
        cache.close()
    }

    routing {
        get("/v1/exchange-rates") {
            val rates =
                try {
                    cache.rates()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    call.respond(HttpStatusCode.BadGateway)
                    return@get
                }
            call.response.header(HttpHeaders.CacheControl, "public, max-age=3600")
            call.respondText(rates, ContentType.Application.Json)
        }
    }
}
