package com.cbgm.securechat.feature.transport.relay.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RelayTransportConfigTest {
    @Test
    fun registryConfigurationDoesNotRequireRelayWebSocketUrl() {
        val config =
            RelayTransportConfig(
                httpBaseUrl = "https://push.example",
                nodeRegistryBaseUrl = "https://registry.example",
                trustedRegistryAuthorityNodeId = "authority"
            )

        assertEquals("https://registry.example", config.nodeRegistryBaseUrl)
        assertEquals(10_000L, config.directoryRefreshIntervalMilliseconds)
        assertEquals(60_000L, config.failedNodeCooldownMilliseconds)
    }

    @Test
    fun invalidRegistryUrlIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            RelayTransportConfig(
                httpBaseUrl = "https://push.example",
                nodeRegistryBaseUrl = "ws://registry.example"
            )
        }
    }
}
