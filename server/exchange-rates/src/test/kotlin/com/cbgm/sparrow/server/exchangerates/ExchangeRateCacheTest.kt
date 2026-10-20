package com.cbgm.sparrow.server.exchangerates

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExchangeRateCacheTest {
    private val rates = """[{"date":"2026-10-21","base":"EUR","quote":"USD","rate":1.08}]"""
    private var now = 0L
    private var requests = 0
    private var status = HttpStatusCode.OK
    private var body = rates

    private val cache =
        ExchangeRateCache(
            upstreamUrl = "https://rates.example/v2/rates?base=EUR",
            ttlMilliseconds = TTL,
            httpClient =
                HttpClient(
                    MockEngine {
                        requests += 1
                        respond(body, status)
                    }
                ),
            clock = { now }
        )

    @Test
    fun `rates are fetched once per cache lifetime`() =
        runTest {
            assertEquals(rates, cache.rates())
            now = TTL - 1
            assertEquals(rates, cache.rates())
            assertEquals(1, requests)

            now = TTL
            cache.rates()
            assertEquals(2, requests)
        }

    @Test
    fun `last rates stay when the upstream fails`() =
        runTest {
            cache.rates()
            now = TTL
            status = HttpStatusCode.ServiceUnavailable

            assertEquals(rates, cache.rates())
        }

    @Test
    fun `a response that is no list of rates is rejected`() =
        runTest {
            body = "<html>maintenance</html>"

            assertFailsWith<Exception> { cache.rates() }
        }

    private companion object {
        const val TTL = 1_000L
    }
}
