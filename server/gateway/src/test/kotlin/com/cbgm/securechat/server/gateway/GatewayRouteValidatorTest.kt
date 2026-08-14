package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned
import com.cbgm.securechat.server.security.ClientRoutingIds
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.Signatures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GatewayRouteValidatorTest {
    @Test
    fun validSignedRouteIsAcceptedWithoutPresenceDirectory() {
        val currentTime = 1_000L
        val identity = NodeIdentity.generate()
        val registration =
            registration(
                identity = identity,
                nodeId = "node-a",
                connectionId = "connection-a",
                expiresAtEpochMilliseconds = currentTime + 90_000L
            )
        val validator =
            GatewayRouteValidator(
                maximumTtlMilliseconds = 90_000L,
                now = { currentTime }
            )

        assertTrue(
            validator.isValid(
                registration = registration,
                connectionRoutingId = registration.route.routingId,
                connectionId = "connection-a",
                expectedNodeId = "node-a"
            )
        )
    }

    @Test
    fun expiredSignedRouteIsRejectedLocally() {
        val currentTime = 100_000L
        val identity = NodeIdentity.generate()
        val registration =
            registration(
                identity = identity,
                nodeId = "node-a",
                connectionId = "connection-a",
                expiresAtEpochMilliseconds = currentTime - 1L
            )
        val validator =
            GatewayRouteValidator(
                maximumTtlMilliseconds = 90_000L,
                now = { currentTime }
            )

        assertFalse(
            validator.isValid(
                registration = registration,
                connectionRoutingId = registration.route.routingId,
                connectionId = "connection-a",
                expectedNodeId = "node-a"
            )
        )
        assertEquals(
            GatewayRouteValidationFailure.EXPIRATION,
            validator.validationFailure(
                registration = registration,
                connectionRoutingId = registration.route.routingId,
                connectionId = "connection-a",
                expectedNodeId = "node-a"
            )
        )
    }

    @Test
    fun routeBeyondMaximumLifetimeIsReportedAsExpirationFailure() {
        val currentTime = 100_000L
        val identity = NodeIdentity.generate()
        val registration =
            registration(
                identity = identity,
                nodeId = "node-a",
                connectionId = "connection-a",
                expiresAtEpochMilliseconds = currentTime + 90_001L
            )
        val validator =
            GatewayRouteValidator(
                maximumTtlMilliseconds = 90_000L,
                now = { currentTime }
            )

        assertEquals(
            GatewayRouteValidationFailure.EXPIRATION,
            validator.validationFailure(
                registration = registration,
                connectionRoutingId = registration.route.routingId,
                connectionId = "connection-a",
                expectedNodeId = "node-a"
            )
        )
    }

    private fun registration(
        identity: NodeIdentity,
        nodeId: String,
        connectionId: String,
        expiresAtEpochMilliseconds: Long
    ): ClientRouteRegistration {
        val signingPublicKey = identity.encodedPublicKey
        val routingId = ClientRoutingIds.fromSigningPublicKey(signingPublicKey)
        val unsignedRoute =
            ClientRoute(
                routingId = routingId,
                nodeId = nodeId,
                connectionId = connectionId,
                generation = 1L,
                expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                aliases = listOf("scphone1_test"),
                clientSignature = byteArrayOf()
            )
        val signature =
            Signatures.sign(
                serverJson.encodeToString(unsignedRoute.unsigned()).encodeToByteArray(),
                identity.privateKey
            )

        return ClientRouteRegistration(
            route = unsignedRoute.copy(clientSignature = signature),
            clientSigningPublicKey = signingPublicKey
        )
    }
}
