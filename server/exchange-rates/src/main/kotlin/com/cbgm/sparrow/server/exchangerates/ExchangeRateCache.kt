package com.cbgm.sparrow.server.exchangerates

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/** Fetches the upstream rates at most once per [ttlMilliseconds]; on failure the last rates stay. */
internal class ExchangeRateCache(
    private val upstreamUrl: String,
    private val ttlMilliseconds: Long,
    private val httpClient: HttpClient = createExchangeRateHttpClient(),
    private val clock: () -> Long = System::currentTimeMillis
) : AutoCloseable {
    private val mutex = Mutex()
    private var rates: String? = null
    private var fetchedAtMilliseconds = 0L

    suspend fun rates(): String =
        mutex.withLock {
            val now = clock()
            val cached = rates
            if (cached != null && now - fetchedAtMilliseconds < ttlMilliseconds) return cached
            try {
                fetch().also { fetched ->
                    rates = fetched
                    fetchedAtMilliseconds = now
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                cached ?: throw failure
            }
        }

    private suspend fun fetch(): String {
        val response = httpClient.get(upstreamUrl)
        check(response.status.isSuccess()) { "Exchange rates failed with ${response.status}" }
        val body = response.bodyAsText()
        // Only a JSON list of rates is passed on to the apps.
        check(Json.parseToJsonElement(body) is JsonArray) { "Exchange rates are not a JSON list" }
        return body
    }

    override fun close() {
        httpClient.close()
    }
}

private fun createExchangeRateHttpClient(): HttpClient =
    HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) { requestTimeoutMillis = REQUEST_TIMEOUT_MILLISECONDS }
    }

private const val REQUEST_TIMEOUT_MILLISECONDS = 10_000L
