package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.ClientRoute
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
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
                assertIs<PresenceResult.Accepted>(store.register(registration, AliasClaims(provenAlias = null, unprovenClaimsAllowed = true)))
                assertEquals(1, store.routeCount())
            }

            createStore(redisUrl, keyPrefix).use { store ->
                assertEquals(listOf(registration.route), store.resolve(routingId).routes)
                assertIs<PresenceResult.Rejected>(
                    store.register(
                        registration(identity, routingId, generation = 1L, now = now),
                        AliasClaims(provenAlias = null, unprovenClaimsAllowed = true)
                    )
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
            val firstClaim = AliasClaims(provenAlias = null, unprovenClaimsAllowed = true)

            createStore(redisUrl, "sparrow:test:presence:$suffix").use { store ->
                assertEquals(
                    PresenceResult.Accepted(listOf(alias)),
                    store.register(registration(owner, ownerRoutingId, 1L, now, listOf(alias)), firstClaim)
                )
                assertEquals(
                    PresenceResult.Accepted(emptyList()),
                    store.register(registration(other, otherRoutingId, 9L, now, listOf(alias)), firstClaim)
                )
                assertEquals(listOf(ownerRoutingId), store.resolve(alias).routes.map(ClientRoute::routingId))
                assertEquals(
                    PresenceResult.Accepted(listOf(alias)),
                    store.register(registration(owner, ownerRoutingId, 2L, now, listOf(alias)), firstClaim)
                )
                assertEquals(
                    PresenceResult.Accepted(listOf(alias)),
                    store.register(
                        registration(other, otherRoutingId, 10L, now, listOf(alias)),
                        firstClaim.copy(provenAlias = alias)
                    )
                )
                assertEquals(listOf(otherRoutingId), store.resolve(alias).routes.map(ClientRoute::routingId))
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
        now: Long,
        aliases: List<String>? = null
    ): ClientRouteRegistration {
        val unsigned =
            ClientRoute(
                routingId = routingId,
                nodeId = "node-a",
                connectionId = "connection-$generation",
                generation = generation,
                expiresAtEpochMilliseconds = now + 60_000L,
                aliases = aliases,
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
