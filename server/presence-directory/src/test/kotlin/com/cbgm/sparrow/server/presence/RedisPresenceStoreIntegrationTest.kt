package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.InvitationRouteResult
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.NodeIdentity
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RedisPresenceStoreIntegrationTest {
    @Test
    fun boundPhoneAliasStaysWithItsDeviceUnlessProvenAndSurvivesARestart() =
        runTest {
            val redisUrl = redisUrl() ?: return@runTest
            val prefix = keyPrefix()
            val alias = ClientRoutingIds.fromPhoneNumber("+491701234567")
            val owner = ClientRoutingIds.fromSigningPublicKey(NodeIdentity.generate().encodedPublicKey)
            val other = ClientRoutingIds.fromSigningPublicKey(NodeIdentity.generate().encodedPublicKey)
            val firstClaim = AliasClaims(provenAlias = alias, aliases = listOf(alias))
            val unproven = AliasClaims(provenAlias = null, aliases = listOf(alias))

            createStore(redisUrl, prefix).use { store ->
                assertEquals(listOf(alias), store.bindAliases(owner, firstClaim))
                assertEquals(emptyList(), store.bindAliases(other, unproven))
            }
            createStore(redisUrl, prefix).use { store ->
                assertEquals(owner, store.aliasOwner(alias))
                assertEquals(listOf(alias), store.bindAliases(owner, unproven))
                assertEquals(listOf(alias), store.bindAliases(other, firstClaim))
                assertEquals(other, store.aliasOwner(alias))
            }
        }

    @Test
    fun theInvitationRouteIsKeptForTheDeviceAndFoundByItsAlias() =
        runTest {
            val redisUrl = redisUrl() ?: return@runTest
            val alias = ClientRoutingIds.fromPhoneNumber("+491701234567")
            val owner = ClientRoutingIds.fromSigningPublicKey(NodeIdentity.generate().encodedPublicKey)
            val invitation = AliasClaimKeysTest.invitationRoute(expiresAt = System.currentTimeMillis() + 60_000L)
            val claim = AliasClaims(provenAlias = alias, aliases = listOf(alias))

            createStore(redisUrl, keyPrefix()).use { store ->
                store.bindAliases(owner, claim)
                store.saveInvitationRoute(owner, invitation)
                assertEquals(InvitationRouteResult(owner, invitation), store.invitationRoute(alias))
                assertEquals(InvitationRouteResult(owner, invitation), store.invitationRoute(owner))
                store.saveInvitationRoute(owner, null)
                assertNull(store.invitationRoute(alias))
            }
        }

    private fun redisUrl(): String? = System.getenv("PRESENCE_TEST_REDIS_URL")?.takeIf(String::isNotBlank)

    private fun keyPrefix(): String = "sparrow:test:presence:${UUID.randomUUID().toString().replace("-", "")}"

    private fun createStore(
        redisUrl: String,
        keyPrefix: String
    ): PresenceStorage = createPresenceStorage(PresenceConfig(redisUrl = redisUrl, redisPassword = null, redisKeyPrefix = keyPrefix))
}
