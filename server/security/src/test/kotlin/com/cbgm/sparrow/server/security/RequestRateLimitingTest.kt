package com.cbgm.sparrow.server.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RequestRateLimitingTest {
    @Test
    fun rejectsExcessRequestsAndResetsAfterWindow() {
        var currentTime = 1_000L
        val limiter =
            BoundedRateLimiter(
                policy = RateLimitPolicy(maximumRequests = 2, windowMilliseconds = 5_000L),
                now = { currentTime }
            )

        assertEquals(RateLimitDecision.Allowed, limiter.acquire("client"))
        assertEquals(RateLimitDecision.Allowed, limiter.acquire("client"))
        assertEquals(5L, assertIs<RateLimitDecision.Rejected>(limiter.acquire("client")).retryAfterSeconds)

        currentTime += 5_000L

        assertEquals(RateLimitDecision.Allowed, limiter.acquire("client"))
    }

    @Test
    fun independentNodeScopesDoNotShareTheSameBucket() {
        val limiter =
            BoundedRateLimiter(
                policy = RateLimitPolicy(maximumRequests = 1, windowMilliseconds = 60_000L)
            )

        assertEquals(RateLimitDecision.Allowed, limiter.acquire("proxy:node-a"))
        assertEquals(RateLimitDecision.Allowed, limiter.acquire("proxy:node-b"))
        assertIs<RateLimitDecision.Rejected>(limiter.acquire("proxy:node-a"))
    }

    @Test
    fun trackedClientCardinalityIsBounded() {
        val limiter =
            BoundedRateLimiter(
                RateLimitPolicy(
                    maximumRequests = 1,
                    windowMilliseconds = 60_000L,
                    maximumTrackedClients = 2
                )
            )

        limiter.acquire("client-1")
        limiter.acquire("client-2")
        limiter.acquire("client-3")

        assertEquals(2, limiter.trackedClientCount())
    }
}
