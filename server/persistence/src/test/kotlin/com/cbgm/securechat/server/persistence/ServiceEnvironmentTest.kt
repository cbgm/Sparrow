package com.cbgm.securechat.server.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServiceEnvironmentTest {
    @Test
    fun secretFileTakesPrecedenceAndTrailingNewlineIsRemoved() {
        val environment =
            mapOf(
                "API_TOKEN" to "environment-token",
                "API_TOKEN_FILE" to "/run/secrets/api-token"
            )

        val result =
            ServiceEnvironment.resolveSecret(
                name = "API_TOKEN",
                environment = environment::get,
                readFile = { "file-token\r\n" }
            )

        assertEquals("file-token", result)
    }

    @Test
    fun environmentValueIsUsedWhenNoSecretFileIsConfigured() {
        val result =
            ServiceEnvironment.resolveSecret(
                name = "API_TOKEN",
                environment = mapOf("API_TOKEN" to "environment-token")::get,
                readFile = { error("No file should be read") }
            )

        assertEquals("environment-token", result)
    }

    @Test
    fun configuredEmptySecretFileFailsClosed() {
        assertFailsWith<IllegalStateException> {
            ServiceEnvironment.resolveSecret(
                name = "API_TOKEN",
                environment = mapOf("API_TOKEN_FILE" to "/run/secrets/api-token")::get,
                readFile = { "\n" }
            )
        }
    }
}

class ControlPlaneEndpointPoolTest {
    @Test
    fun failedEndpointIsDeprioritizedUntilCooldownExpires() {
        var now = 1_000L
        val pool =
            ControlPlaneEndpointPool(
                baseUrls = listOf("https://cp-a.example", "https://cp-b.example"),
                failureCooldownMilliseconds = 5_000L,
                now = { now }
            )

        pool.markUnavailable("https://cp-a.example")

        assertEquals(
            listOf("https://cp-b.example", "https://cp-a.example"),
            pool.ordered()
        )

        now += 5_001L

        assertEquals(
            listOf("https://cp-a.example", "https://cp-b.example"),
            pool.ordered()
        )
    }

    @Test
    fun successfulEndpointBecomesPreferred() {
        val pool =
            ControlPlaneEndpointPool(
                baseUrls = listOf("https://cp-a.example", "https://cp-b.example")
            )

        pool.markAvailable("https://cp-b.example")

        assertEquals(
            listOf("https://cp-b.example", "https://cp-a.example"),
            pool.ordered()
        )
    }

    @Test
    fun unavailableEndpointsAreExcludedFromReplicationUntilCooldownExpires() {
        var now = 1_000L
        val pool =
            ControlPlaneEndpointPool(
                baseUrls = listOf("https://cp-a.example", "https://cp-b.example"),
                failureCooldownMilliseconds = 5_000L,
                now = { now }
            )

        pool.markUnavailable("https://cp-a.example")

        assertEquals(
            listOf("https://cp-b.example"),
            pool.availableEndpoints()
        )

        now += 5_001L

        assertEquals(
            listOf("https://cp-a.example", "https://cp-b.example"),
            pool.availableEndpoints()
        )
    }
}
