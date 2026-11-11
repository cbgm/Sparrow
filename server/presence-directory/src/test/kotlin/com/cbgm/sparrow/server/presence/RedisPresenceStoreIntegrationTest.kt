package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.InvitationRouteResult
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.protocol.unsigned
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.NodeIdentity
import com.cbgm.sparrow.server.security.Signatures
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class RedisPresenceStoreIntegrationTest {
    @Test
    fun routeAndGenerationSurviveStoreRecreation() =
        runTest {
            val redisUrl =
                System.getenv("PRESENCE_TEST_REDIS_URL")
                    ?.takeIf(String::isNotBlank)
                    ?: return@runTest
            val suffix = UUID.randomUUID().toString().replace("-", "")
            val keyPrefix = "sparrow:test:presence:$suffix"
            val identity = NodeIdentity.generate()
            val routingId = ClientRoutingIds.fromSigningPublicKey(identity.encodedPublicKey)
            val now = System.currentTimeMillis()
            val registration = registration(identity, routingId, generation = 2L, now = now)

            createStore(redisUrl, keyPrefix).use { store ->
                assertEquals(PresenceResult.Accepted, store.register(registration))
                assertEquals(1, store.routeCount())
            }

            createStore(redisUrl, keyPrefix).use { store ->
                assertEquals(listOf(registration.route), store.resolve(routingId).routes)
                assertIs<PresenceResult.Rejected>(
                    store.register(registration(identity, routingId, generation = 1L, now = now))
                )
                store.remove(routingId, registration.route.connectionId)
                assertEquals(emptyList(), store.resolve(routingId).routes)
            }
        }

    @Test
    fun boundPhoneAliasStaysWithItsDeviceUnlessProven() =
        runTest {
            val redisUrl =
                System.getenv("PRESENCE_TEST_REDIS_URL")
                    ?.takeIf(String::isNotBlank)
                    ?: return@runTest
            val suffix = UUID.randomUUID().toString().replace("-", "")
            val alias = ClientRoutingIds.fromPhoneNumber("+491701234567")
            val now = System.currentTimeMillis()
            val owner = NodeIdentity.generate()
            val ownerRoutingId = ClientRoutingIds.fromSigningPublicKey(owner.encodedPublicKey)
            val other = NodeIdentity.generate()
            val otherRoutingId = ClientRoutingIds.fromSigningPublicKey(other.encodedPublicKey)
            val firstClaim = AliasClaims(provenAlias = alias, aliases = listOf(alias))
            val unproven = AliasClaims(provenAlias = null, aliases = listOf(alias))

            createStore(redisUrl, "sparrow:test:presence:$suffix").use { store ->
                store.register(registration(owner, ownerRoutingId, 1L, now))
                assertEquals(listOf(alias), store.bindAliases(ownerRoutingId, firstClaim))
                assertEquals(emptyList(), store.bindAliases(otherRoutingId, unproven))
                assertEquals(ownerRoutingId, store.resolve(alias).ownerRoutingId)
                assertEquals(listOf(ownerRoutingId), store.resolve(alias).routes.map(ClientRoute::routingId))
                assertEquals(listOf(alias), store.bindAliases(ownerRoutingId, unproven))
                assertEquals(listOf(alias), store.bindAliases(otherRoutingId, firstClaim))
                assertEquals(otherRoutingId, store.resolve(alias).ownerRoutingId)
            }
        }

    @Test
    fun theInvitationRouteIsKeptWithTheAliasBinding() =
        runTest {
            val redisUrl =
                System.getenv("PRESENCE_TEST_REDIS_URL")
                    ?.takeIf(String::isNotBlank)
                    ?: return@runTest
            val suffix = UUID.randomUUID().toString().replace("-", "")
            val alias = ClientRoutingIds.fromPhoneNumber("+491701234567")
            val owner = ClientRoutingIds.fromSigningPublicKey(NodeIdentity.generate().encodedPublicKey)
            val invitation = AliasClaimKeysTest.invitationRoute(expiresAt = System.currentTimeMillis() + 60_000L)
            val claim = AliasClaims(provenAlias = alias, aliases = listOf(alias))

            createStore(redisUrl, "sparrow:test:presence:$suffix").use { store ->
                store.bindAliases(owner, claim)
                store.saveInvitationRoute(owner, invitation)
                assertEquals(InvitationRouteResult(owner, invitation), store.invitationRoute(alias))
                assertEquals(InvitationRouteResult(owner, invitation), store.invitationRoute(owner))
                store.saveInvitationRoute(owner, null)
                assertNull(store.invitationRoute(alias))
            }
        }

    private fun createStore(
        redisUrl: String,
        keyPrefix: String
    ): PresenceStorage =
        createPresenceStorage(
            PresenceConfig(
                redisUrl = redisUrl,
                redisPassword = null,
                redisKeyPrefix = keyPrefix,
                maximumTtlMilliseconds = 120_000L
            )
        )

    private fun registration(
        identity: NodeIdentity,
        routingId: String,
        generation: Long,
        now: Long
    ): ClientRouteRegistration {
        val unsigned =
            ClientRoute(
                routingId = routingId,
                nodeId = "node-a",
                connectionId = "connection-$generation",
                generation = generation,
                expiresAtEpochMilliseconds = now + 60_000L,
                clientSignature = byteArrayOf()
            )
        val signature =
            Signatures.sign(
                serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                identity.privateKey
            )
        return ClientRouteRegistration(
            route = unsigned.copy(clientSignature = signature),
            clientSigningPublicKey = identity.encodedPublicKey
        )
    }
}
