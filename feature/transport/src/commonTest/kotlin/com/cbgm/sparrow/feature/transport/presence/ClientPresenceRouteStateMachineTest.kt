package com.cbgm.sparrow.feature.transport.presence

import com.cbgm.sparrow.feature.transport.gateway.model.ClientRoute
import com.cbgm.sparrow.feature.transport.gateway.model.ClientRouteRegistration
import com.cbgm.sparrow.feature.transport.gateway.model.GatewayNodeInformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClientPresenceRouteStateMachineTest {
    @Test
    fun gatewayRegistrationStartsPresencePreparationImmediately() {
        val gatewayInformation = gatewayInformation()

        val transition =
            ClientPresenceRouteStateMachine.transition(
                current = ClientPresenceRouteState.AwaitingGatewayRegistration,
                event = ClientPresenceRouteEvent.GatewayRegistered(gatewayInformation)
            )

        assertIs<ClientPresenceRouteState.PreparingRegistration>(transition.state)
        assertEquals(
            listOf(ClientPresenceRouteEffect.PrepareRegistration(gatewayInformation)),
            transition.effects
        )
    }

    @Test
    fun preparedRegistrationIsPublishedImmediately() {
        val gatewayInformation = gatewayInformation()
        val registration = registration()

        val transition =
            ClientPresenceRouteStateMachine.transition(
                current = ClientPresenceRouteState.PreparingRegistration(gatewayInformation),
                event = ClientPresenceRouteEvent.RegistrationPrepared(registration)
            )

        assertIs<ClientPresenceRouteState.PublishingRoute>(transition.state)
        assertEquals(
            listOf(ClientPresenceRouteEffect.PublishRoute(registration)),
            transition.effects
        )
    }

    @Test
    fun gatewayAcceptanceMakesRouteReadyAndSchedulesProtocolRefresh() {
        val gatewayInformation = gatewayInformation()
        val registration = registration()
        val aliases = setOf("bootstrap-a")

        val transition =
            ClientPresenceRouteStateMachine.transition(
                current =
                    ClientPresenceRouteState.PublishingRoute(
                        gatewayInformation = gatewayInformation,
                        registration = registration
                    ),
                event = ClientPresenceRouteEvent.RouteAccepted(aliases)
            )

        val ready = assertIs<ClientPresenceRouteState.Ready>(transition.state)
        assertEquals(aliases, ready.aliases)
        assertEquals(
            listOf(
                ClientPresenceRouteEffect.ScheduleRefresh(
                    gatewayInformation.routeRefreshIntervalMilliseconds
                )
            ),
            transition.effects
        )
    }

    @Test
    fun refreshDueImmediatelyPreparesNextSignedRoute() {
        val gatewayInformation = gatewayInformation()

        val transition =
            ClientPresenceRouteStateMachine.transition(
                current = ClientPresenceRouteState.Ready(gatewayInformation, setOf("bootstrap-a")),
                event = ClientPresenceRouteEvent.RefreshDue
            )

        assertIs<ClientPresenceRouteState.PreparingRegistration>(transition.state)
        assertEquals(
            listOf(ClientPresenceRouteEffect.PrepareRegistration(gatewayInformation)),
            transition.effects
        )
    }

    @Test
    fun routeRejectionMovesToFailed() {
        val gatewayInformation = gatewayInformation()
        val registration = registration()
        val error = IllegalStateException("rejected")

        val transition =
            ClientPresenceRouteStateMachine.transition(
                current =
                    ClientPresenceRouteState.PublishingRoute(
                        gatewayInformation = gatewayInformation,
                        registration = registration
                    ),
                event = ClientPresenceRouteEvent.RouteRejected(error)
            )

        val failed = assertIs<ClientPresenceRouteState.Failed>(transition.state)
        assertEquals(error, failed.error)
        assertEquals(listOf(ClientPresenceRouteEffect.Fail(error)), transition.effects)
    }

    @Test
    fun routeExpirationUsesGatewayClockWhenAvailable() {
        val gatewayInformation =
            gatewayInformation(
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
    fun cachedGatewayClockAdvancesWhileConnectionRemainsOpen() {
        val gatewayInformation =
            gatewayInformation(
                serverTimeEpochMilliseconds = 1_000_000L,
                serverTimeObservedAtEpochMilliseconds = 2_000_000L
            )

        val expiration =
            routeExpirationEpochMilliseconds(
                gatewayInformation = gatewayInformation,
                localNowEpochMilliseconds = 2_060_000L
            )

        assertEquals(1_150_000L, expiration)
    }

    @Test
    fun legacyGatewayLeavesClockSkewSafetyMargin() {
        val localNow = 1_000_000L
        val gatewayInformation = gatewayInformation()

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

    private fun gatewayInformation(
        serverTimeEpochMilliseconds: Long? = null,
        serverTimeObservedAtEpochMilliseconds: Long? = null
    ): GatewayNodeInformation =
        GatewayNodeInformation(
            nodeId = "node-a",
            routeLifetimeMilliseconds = 90_000L,
            routeRefreshIntervalMilliseconds = 30_000L,
            serverTimeEpochMilliseconds = serverTimeEpochMilliseconds,
            serverTimeObservedAtEpochMilliseconds = serverTimeObservedAtEpochMilliseconds
        )

    private fun registration(): ClientRouteRegistration =
        ClientRouteRegistration(
            route =
                ClientRoute(
                    routingId = "routing-a",
                    nodeId = "node-a",
                    connectionId = "connection-a",
                    generation = 1L,
                    expiresAtEpochMilliseconds = 2L,
                    aliases = listOf("bootstrap-a"),
                    clientSignature = byteArrayOf(1)
                ),
            clientSigningPublicKey = byteArrayOf(2)
        )
}
