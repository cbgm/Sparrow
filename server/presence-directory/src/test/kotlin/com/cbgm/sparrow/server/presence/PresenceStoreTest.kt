package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.InvitationRouteResult
import com.cbgm.sparrow.server.security.ClientRoutingIds
import com.cbgm.sparrow.server.security.NodeIdentity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PresenceStoreTest {
    @Test
    fun aProvenAliasIsBoundToItsDevice() =
        runTest {
            val owner = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })

            assertEquals(listOf(ALIAS), store.bindAliases(owner.routingId, FIRST_CLAIM))

            assertEquals(owner.routingId, store.aliasOwner(ALIAS))
        }

    @Test
    fun anotherDeviceCannotTakeABoundPhoneAlias() =
        runTest {
            val owner = NodeIdentity.generate()
            val attacker = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })

            assertEquals(listOf(ALIAS), store.bindAliases(owner.routingId, FIRST_CLAIM))
            assertEquals(emptyList(), store.bindAliases(attacker.routingId, UNPROVEN_CLAIM))

            assertEquals(owner.routingId, store.aliasOwner(ALIAS))
        }

    @Test
    fun ownerKeepsItsAliasWithoutProofAndAProofMovesIt() =
        runTest {
            val owner = NodeIdentity.generate()
            val newDevice = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })

            assertEquals(emptyList(), store.bindAliases(owner.routingId, UNPROVEN_CLAIM))
            store.bindAliases(owner.routingId, FIRST_CLAIM)
            assertEquals(listOf(ALIAS), store.bindAliases(owner.routingId, UNPROVEN_CLAIM))
            assertEquals(listOf(ALIAS), store.bindAliases(newDevice.routingId, FIRST_CLAIM))

            assertEquals(newDevice.routingId, store.aliasOwner(ALIAS))
        }

    @Test
    fun aliasBindingExpiresAfterTheOwnerStopsClaimingIt() =
        runTest {
            var now = 1_000L
            val store = PresenceStore(now = { now })
            store.bindAliases(NodeIdentity.generate().routingId, FIRST_CLAIM)

            now += ALIAS_BINDING_LIFETIME_MILLISECONDS
            assertNull(store.aliasOwner(ALIAS))
        }

    @Test
    fun theInvitationRouteIsFoundByTheDeviceAndByItsAliasWhileTheMailboxLives() =
        runTest {
            var now = 1_000L
            val owner = NodeIdentity.generate()
            val store = PresenceStore(now = { now })
            val invitation = invitationRoute(expiresAt = 5_000L)
            store.bindAliases(owner.routingId, FIRST_CLAIM)

            store.saveInvitationRoute(owner.routingId, invitation)

            assertEquals(InvitationRouteResult(owner.routingId, invitation), store.invitationRoute(ALIAS))
            assertEquals(InvitationRouteResult(owner.routingId, invitation), store.invitationRoute(owner.routingId))
            now = 5_000L
            assertNull(store.invitationRoute(ALIAS))
        }

    @Test
    fun aClaimWithoutInvitationRouteRemovesTheOldOne() =
        runTest {
            val owner = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })
            store.saveInvitationRoute(owner.routingId, invitationRoute(expiresAt = 5_000L))

            store.saveInvitationRoute(owner.routingId, null)

            assertNull(store.invitationRoute(owner.routingId))
        }

    @Test
    fun anAliasLeadsToTheRouteOfTheDeviceHoldingIt() =
        runTest {
            val owner = NodeIdentity.generate()
            val other = NodeIdentity.generate()
            val store = PresenceStore(now = { 1_000L })
            val invitation = invitationRoute(expiresAt = 5_000L)
            store.bindAliases(owner.routingId, FIRST_CLAIM)
            store.saveInvitationRoute(owner.routingId, invitation)

            store.bindAliases(other.routingId, UNPROVEN_CLAIM)
            store.saveInvitationRoute(other.routingId, invitationRoute(expiresAt = 6_000L))

            assertEquals(InvitationRouteResult(owner.routingId, invitation), store.invitationRoute(ALIAS))
        }

    private fun invitationRoute(expiresAt: Long) = AliasClaimKeysTest.invitationRoute(expiresAt)

    private val NodeIdentity.routingId: String
        get() = ClientRoutingIds.fromSigningPublicKey(encodedPublicKey)

    private companion object {
        val ALIAS = ClientRoutingIds.fromPhoneNumber("+491701234567")
        val FIRST_CLAIM = AliasClaims(provenAlias = ALIAS, aliases = listOf(ALIAS))
        val UNPROVEN_CLAIM = AliasClaims(provenAlias = null, aliases = listOf(ALIAS))
    }
}
