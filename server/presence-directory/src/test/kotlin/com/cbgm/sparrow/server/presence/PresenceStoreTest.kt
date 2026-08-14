package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.protocol.unsigned
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.Signatures
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PresenceStoreTest {
    @Test
    fun olderGenerationCannotReplaceNewRoute() =
        runTest {
            val identity = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })

            assertIs<PresenceResult.Accepted>(store.register(registration(identity, generation = 2L)))
            assertIs<PresenceResult.Rejected>(store.register(registration(identity, generation = 1L)))
            val routingId = ClientRoutingIds.fromSigningPublicKey(identity.encodedPublicKey)
            assertEquals(2L, store.resolve(routingId).routes.single().generation)
        }

    @Test
    fun signingKeyCannotClaimAnotherDeviceRoutingId() =
        runTest {
            val identity = NodeIdentity.generate()
            val registration = registration(identity, generation = 1L)
            val claimedRoutingId = ClientRoutingIds.fromSigningPublicKey(ByteArray(32) { 7 })
            val result =
                PresenceStore(now = { 1_000L }).register(
                    registration.copy(
                        route = registration.route.copy(routingId = claimedRoutingId)
                    )
                )

            assertEquals(PresenceResult.Rejected("INVALID_ROUTING_ID"), result)
        }

    private fun registration(
        identity: NodeIdentity,
        generation: Long
    ): ClientRouteRegistration {
        val unsigned =
            ClientRoute(
                routingId = ClientRoutingIds.fromSigningPublicKey(identity.encodedPublicKey),
                nodeId = "node-a",
                connectionId = "connection-$generation",
                generation = generation,
                expiresAtEpochMilliseconds = 2_000L,
                clientSignature = byteArrayOf()
            )
        val signature =
            Signatures.sign(
                serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                identity.privateKey
            )
        return ClientRouteRegistration(unsigned.copy(clientSignature = signature), identity.encodedPublicKey)
    }
}
