package com.cbgm.securechat.feature.transport.relay.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RelayTransportConfigTest {
    @Test
    fun defaultTransportTimingIsValid() {
        val config =
            RelayTransportConfig(
                trustedRegistryRootNodeId = "authority"
            )

        assertEquals(10_000L, config.directoryRefreshIntervalMilliseconds)
        assertEquals(60_000L, config.failedNodeCooldownMilliseconds)
    }

    @Test
    fun blankAuthorityNodeIdIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            RelayTransportConfig(
                trustedRegistryRootNodeId = ""
            )
        }
    }
}
