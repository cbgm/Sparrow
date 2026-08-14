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
                assertIs<PresenceResult.Accepted>(store.register(registration))
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
