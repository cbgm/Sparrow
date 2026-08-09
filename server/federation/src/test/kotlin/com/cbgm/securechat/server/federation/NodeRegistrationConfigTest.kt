package com.cbgm.securechat.server.federation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeRegistrationConfigTest {
    @Test
    fun defaultsKeepLargeDescriptorSafetyWindow() {
        val config =
            NodeRegistrationConfig(
                registryUrl = "https://registry.example",
                clientEndpoint = "wss://node.example/relay",
                federationEndpoint = "https://node.example/federation",
                mailboxEndpoint = "https://node.example/mailbox"
            )

        assertEquals(60L * 60L * 1_000L, config.descriptorLifetimeMilliseconds)
        assertEquals(10L * 60L * 1_000L, config.registrationRefreshMilliseconds)
        assertEquals(5_000L, config.heartbeatIntervalMilliseconds)
        assertTrue(
            config.descriptorLifetimeMilliseconds >=
                config.registrationRefreshMilliseconds * MINIMUM_SAFETY_FACTOR
        )
    }

    private companion object {
        const val MINIMUM_SAFETY_FACTOR = 6L
    }
}
