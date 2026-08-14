package com.cbgm.sparrow.feature.transport.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TransportConfigTest {
    @Test
    fun defaultTransportTimingIsValid() {
        val config =
            TransportConfig(
                trustedRegistryRootNodeId = "authority"
            )

        assertEquals(10_000L, config.directoryRefreshIntervalMilliseconds)
        assertEquals(60_000L, config.failedNodeCooldownMilliseconds)
    }

    @Test
    fun blankAuthorityNodeIdIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            TransportConfig(
                trustedRegistryRootNodeId = ""
            )
        }
    }
}
