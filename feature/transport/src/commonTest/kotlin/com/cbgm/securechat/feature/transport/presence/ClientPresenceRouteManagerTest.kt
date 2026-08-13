package com.cbgm.securechat.feature.transport.presence

import com.cbgm.securechat.feature.transport.gateway.model.GatewayNodeInformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientPresenceRouteManagerTest {
    @Test
    fun routeExpirationUsesGatewayClockWhenAvailable() {
        val gatewayInformation =
            GatewayNodeInformation(
                nodeId = "node-a",
                routeLifetimeMilliseconds = 90_000L,
                routeRefreshIntervalMilliseconds = 30_000L,
                serverTimeEpochMilliseconds = 1_000_000L
            )

        val expiration =
            routeExpirationEpochMilliseconds(
                gatewayInformation = gatewayInformation,
                localNowEpochMilliseconds = 9_000_000L
            )

        assertEquals(1_090_000L, expiration)
    }

    @Test
    fun legacyGatewayLeavesClockSkewSafetyMargin() {
        val localNow = 1_000_000L
        val gatewayInformation =
            GatewayNodeInformation(
                nodeId = "node-a",
                routeLifetimeMilliseconds = 90_000L,
                routeRefreshIntervalMilliseconds = 30_000L
            )

        val expiration =
            routeExpirationEpochMilliseconds(
                gatewayInformation = gatewayInformation,
                localNowEpochMilliseconds = localNow
            )

        assertEquals(localNow + 60_000L, expiration)
    }

    @Test
    fun legacySafetyMarginNeverExpiresBeforeFirstRefresh() {
        val localNow = 1_000_000L
        val gatewayInformation =
            GatewayNodeInformation(
                nodeId = "node-a",
                routeLifetimeMilliseconds = 40_000L,
                routeRefreshIntervalMilliseconds = 35_000L
            )

        val expiration =
            routeExpirationEpochMilliseconds(
                gatewayInformation = gatewayInformation,
                localNowEpochMilliseconds = localNow
            )

        assertTrue(
            expiration > localNow + gatewayInformation.routeRefreshIntervalMilliseconds
        )
    }
}
