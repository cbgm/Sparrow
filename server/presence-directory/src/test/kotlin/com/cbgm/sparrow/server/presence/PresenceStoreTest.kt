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

            assertIs<PresenceResult.Accepted>(store.register(registration(identity, generation = 2L), FIRST_CLAIM))
            assertIs<PresenceResult.Rejected>(store.register(registration(identity, generation = 1L), FIRST_CLAIM))
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
                    ),
                    FIRST_CLAIM
                )

            assertEquals(PresenceResult.Rejected("INVALID_ROUTING_ID"), result)
        }

    @Test
    fun anotherDeviceCannotTakeABoundPhoneAlias() =
        runTest {
            val owner = NodeIdentity.generate()
            val attacker = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })

            assertEquals(
                PresenceResult.Accepted(listOf(ALIAS)),
                store.register(registration(owner, generation = 1L, aliases = listOf(ALIAS)), FIRST_CLAIM)
            )
            assertEquals(
                PresenceResult.Accepted(emptyList()),
                store.register(registration(attacker, generation = 9L, aliases = listOf(ALIAS)), FIRST_CLAIM)
            )

            val ownerRoutingId = ClientRoutingIds.fromSigningPublicKey(owner.encodedPublicKey)
            assertEquals(listOf(ownerRoutingId), store.resolve(ALIAS).routes.map(ClientRoute::routingId))
            val attackerRoutingId = ClientRoutingIds.fromSigningPublicKey(attacker.encodedPublicKey)
            assertEquals(1, store.resolve(attackerRoutingId).routes.size)
        }

    @Test
    fun removingARouteWithACopiedConnectionIdKeepsTheOwnersAlias() =
        runTest {
            val owner = NodeIdentity.generate()
            val attacker = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })
            store.register(registration(owner, generation = 1L, aliases = listOf(ALIAS)), FIRST_CLAIM)
            store.register(registration(attacker, generation = 1L, aliases = listOf(ALIAS)), FIRST_CLAIM)

            store.remove(ClientRoutingIds.fromSigningPublicKey(attacker.encodedPublicKey), "connection-1")

            assertEquals(1, store.resolve(ALIAS).routes.size)
        }

    @Test
    fun ownerKeepsItsAliasWithoutProofAndAProofMovesIt() =
        runTest {
            val owner = NodeIdentity.generate()
            val newDevice = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })
            val verifiedServer = AliasClaims(provenAlias = null, unprovenClaimsAllowed = false)

            assertEquals(
                PresenceResult.Accepted(emptyList()),
                store.register(registration(owner, generation = 1L, aliases = listOf(ALIAS)), verifiedServer)
            )
            store.register(
                registration(owner, generation = 2L, aliases = listOf(ALIAS)),
                verifiedServer.copy(provenAlias = ALIAS)
            )
            assertEquals(
                PresenceResult.Accepted(listOf(ALIAS)),
                store.register(registration(owner, generation = 3L, aliases = listOf(ALIAS)), verifiedServer)
            )
            assertEquals(
                PresenceResult.Accepted(listOf(ALIAS)),
                store.register(
                    registration(newDevice, generation = 1L, aliases = listOf(ALIAS)),
                    verifiedServer.copy(provenAlias = ALIAS)
                )
            )

            val newRoutingId = ClientRoutingIds.fromSigningPublicKey(newDevice.encodedPublicKey)
            assertEquals(listOf(newRoutingId), store.resolve(ALIAS).routes.map(ClientRoute::routingId))
        }

    @Test
    fun aliasBindingExpiresAfterTheOwnerStopsRegistering() =
        runTest {
            var now = 1_000L
            val store = PresenceStore(now = { now })
            store.register(registration(NodeIdentity.generate(), generation = 1L, aliases = listOf(ALIAS), now = now), FIRST_CLAIM)

            now += ALIAS_BINDING_LIFETIME_MILLISECONDS
            val successor = NodeIdentity.generate()
            assertEquals(
                PresenceResult.Accepted(listOf(ALIAS)),
                store.register(registration(successor, generation = 1L, aliases = listOf(ALIAS), now = now), FIRST_CLAIM)
            )
        }

    private fun registration(
        identity: NodeIdentity,
        generation: Long,
        aliases: List<String>? = null,
        now: Long = 1_000L
    ): ClientRouteRegistration {
        val unsigned =
            ClientRoute(
                routingId = ClientRoutingIds.fromSigningPublicKey(identity.encodedPublicKey),
                nodeId = "node-a",
                connectionId = "connection-$generation",
                generation = generation,
                expiresAtEpochMilliseconds = now + 1_000L,
                aliases = aliases,
                clientSignature = byteArrayOf()
            )
        val signature =
            Signatures.sign(
                serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                identity.privateKey
            )
        return ClientRouteRegistration(unsigned.copy(clientSignature = signature), identity.encodedPublicKey)
    }

    private companion object {
        val ALIAS = ClientRoutingIds.fromPhoneNumber("+491701234567")
        val FIRST_CLAIM = AliasClaims(provenAlias = null, unprovenClaimsAllowed = true)
    }
}
