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
import kotlin.test.assertNull

class PresenceStoreTest {
    @Test
    fun olderGenerationCannotReplaceNewRoute() =
        runTest {
            val identity = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })

            assertEquals(PresenceResult.Accepted, store.register(registration(identity, generation = 2L)))
            assertIs<PresenceResult.Rejected>(store.register(registration(identity, generation = 1L)))
            assertEquals(2L, store.resolve(identity.routingId).routes.single().generation)
        }

    @Test
    fun signingKeyCannotClaimAnotherDeviceRoutingId() =
        runTest {
            val registration = registration(NodeIdentity.generate(), generation = 1L)
            val claimedRoutingId = ClientRoutingIds.fromSigningPublicKey(ByteArray(32) { 7 })
            val result =
                PresenceStore(now = { 1_000L }).register(
                    registration.copy(route = registration.route.copy(routingId = claimedRoutingId))
                )

            assertEquals(PresenceResult.Rejected("INVALID_ROUTING_ID"), result)
        }

    @Test
    fun anAliasResolvesToTheRoutesOfTheDeviceHoldingIt() =
        runTest {
            val owner = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })
            store.register(registration(owner, generation = 1L))

            assertEquals(listOf(ALIAS), store.bindAliases(owner.routingId, FIRST_CLAIM))

            val resolved = store.resolve(ALIAS)
            assertEquals(owner.routingId, resolved.ownerRoutingId)
            assertEquals(listOf(owner.routingId), resolved.routes.map(ClientRoute::routingId))
            assertNull(resolved.routes.single().aliases)
        }

    @Test
    fun anAliasIsBoundEvenWhenTheDeviceRouteIsAtAnotherControlPlane() =
        runTest {
            val owner = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })

            store.bindAliases(owner.routingId, FIRST_CLAIM)

            val resolved = store.resolve(ALIAS)
            assertEquals(owner.routingId, resolved.ownerRoutingId)
            assertEquals(emptyList(), resolved.routes)
        }

    @Test
    fun anotherDeviceCannotTakeABoundPhoneAlias() =
        runTest {
            val owner = NodeIdentity.generate()
            val attacker = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })

            assertEquals(listOf(ALIAS), store.bindAliases(owner.routingId, FIRST_CLAIM))
            assertEquals(emptyList(), store.bindAliases(attacker.routingId, FIRST_CLAIM))

            assertEquals(owner.routingId, store.resolve(ALIAS).ownerRoutingId)
        }

    @Test
    fun ownerKeepsItsAliasWithoutProofAndAProofMovesIt() =
        runTest {
            val owner = NodeIdentity.generate()
            val newDevice = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })
            val verifiedServer = AliasClaims(provenAlias = null, unprovenClaimsAllowed = false, aliases = listOf(ALIAS))

            assertEquals(emptyList(), store.bindAliases(owner.routingId, verifiedServer))
            store.bindAliases(owner.routingId, verifiedServer.copy(provenAlias = ALIAS))
            assertEquals(listOf(ALIAS), store.bindAliases(owner.routingId, verifiedServer))
            assertEquals(listOf(ALIAS), store.bindAliases(newDevice.routingId, verifiedServer.copy(provenAlias = ALIAS)))

            assertEquals(newDevice.routingId, store.resolve(ALIAS).ownerRoutingId)
        }

    @Test
    fun aliasBindingExpiresAfterTheOwnerStopsClaimingIt() =
        runTest {
            var now = 1_000L
            val store = PresenceStore(now = { now })
            store.bindAliases(NodeIdentity.generate().routingId, FIRST_CLAIM)

            now += ALIAS_BINDING_LIFETIME_MILLISECONDS
            val successor = NodeIdentity.generate()
            assertEquals(listOf(ALIAS), store.bindAliases(successor.routingId, FIRST_CLAIM))
        }

    @Test
    fun aliasesTheRouteItselfNamesAreIgnored() =
        runTest {
            val store = PresenceStore(now = { 1_000L })

            // Older apps named their aliases in the route, which the gateway can read.
            assertEquals(
                PresenceResult.Accepted,
                store.register(registration(NodeIdentity.generate(), generation = 1L, aliases = listOf(ALIAS)))
            )
            assertEquals(emptyList(), store.resolve(ALIAS).routes)
        }

    private val NodeIdentity.routingId: String
        get() = ClientRoutingIds.fromSigningPublicKey(encodedPublicKey)

    private fun registration(
        identity: NodeIdentity,
        generation: Long,
        aliases: List<String>? = null,
        now: Long = 1_000L
    ): ClientRouteRegistration {
        val unsigned =
            ClientRoute(
                routingId = identity.routingId,
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
        val FIRST_CLAIM = AliasClaims(provenAlias = null, unprovenClaimsAllowed = true, aliases = listOf(ALIAS))
    }
}
