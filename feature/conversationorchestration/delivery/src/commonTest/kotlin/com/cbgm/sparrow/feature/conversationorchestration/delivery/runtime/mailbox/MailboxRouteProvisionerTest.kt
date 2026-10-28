package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.protocol.packet.MailboxRoutePacket
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Renewing a mailbox must not lose envelopes, and removed contacts never get a fresh route. */
class MailboxRouteProvisionerTest {
    private val fakes = ProvisionerFakes()

    @Test
    fun renewalKeepsTheOldMailboxReadableInsteadOfRevokingIt() =
        runTest {
            val old = mailboxCredential(CONTACT_ID, "old", expiresAt = SystemClock.nowEpochMilliseconds() + DAY)
            fakes.routes.local[CONTACT_ID] = old
            fakes.contacts.facts = listOf(facts(CONTACT_ID, sharesActiveGroup = true))

            assertEquals(1, fakes.provisioner().provision().getOrThrow())

            val renewed = fakes.routes.local.getValue(CONTACT_ID)
            assertNotEquals(old.deliveryRoute.routeId, renewed.deliveryRoute.routeId)
            assertEquals(listOf(old), fakes.routes.retiring.values.toList())
            assertTrue(fakes.gateway.revoked.isEmpty())
            val packet = fakes.outbox.enqueued.single().second as MailboxRoutePacket
            assertEquals(renewed.deliveryRoute.routeId, packet.deliveryRoute.routeId)
        }

    @Test
    fun expiredRetiringMailboxIsRevokedOnlyOnceItIsEmpty() =
        runTest {
            val expired = SystemClock.nowEpochMilliseconds() - 1
            val drained = mailboxCredential(CONTACT_ID, "drained", expiresAt = expired)
            val full = mailboxCredential(OTHER_CONTACT_ID, "full", expiresAt = expired)
            val current = mailboxCredential(CONTACT_ID, "current", expiresAt = Long.MAX_VALUE)
            fakes.routes.retiring[drained.deliveryRoute.routeId] = drained
            fakes.routes.retiring[full.deliveryRoute.routeId] = full
            fakes.routes.local[CONTACT_ID] = current
            fakes.gateway.pendingRoutes += full.deliveryRoute.routeId

            fakes.provisioner().provision().getOrThrow()

            assertEquals(listOf(drained.deliveryRoute.routeId), fakes.gateway.revoked)
            assertEquals(listOf(full), fakes.routes.retiring.values.toList())
            assertEquals(current, fakes.routes.local[CONTACT_ID])
        }

    @Test
    fun retiringMailboxIsNotRevokedBeforeItExpires() =
        runTest {
            val retiring = mailboxCredential(CONTACT_ID, "retiring", expiresAt = SystemClock.nowEpochMilliseconds() + DAY)
            fakes.routes.retiring[retiring.deliveryRoute.routeId] = retiring

            fakes.provisioner().provision().getOrThrow()

            assertTrue(fakes.gateway.revoked.isEmpty())
            assertEquals(listOf(retiring), fakes.routes.retiring.values.toList())
        }

    @Test
    fun mailboxOnRetiredNodeIsReplacedWithoutRevocationOrRetiringCopy() =
        runTest {
            val old = mailboxCredential(CONTACT_ID, "old", expiresAt = Long.MAX_VALUE, nodeId = "node-gone")
            fakes.routes.local[CONTACT_ID] = old
            fakes.contacts.facts = listOf(facts(CONTACT_ID, sharesActiveGroup = true))

            assertEquals(1, fakes.provisioner().provision().getOrThrow())

            assertNotEquals(old, fakes.routes.local.getValue(CONTACT_ID))
            assertTrue(fakes.routes.retiring.isEmpty())
            assertTrue(fakes.gateway.revoked.isEmpty())
        }

    @Test
    fun contactWithoutChatOrSharedGroupGetsNoRoute() =
        runTest {
            // Deleting the chat closed the exchange and removed the conversation; the contact stays MUTUAL.
            fakes.contacts.facts = listOf(facts(CONTACT_ID))
            fakes.identity.establishedPeers += CONTACT_ID

            assertEquals(0, fakes.provisioner().provision().getOrThrow())

            assertTrue(fakes.gateway.created.isEmpty())
            assertTrue(fakes.outbox.enqueued.isEmpty())
        }

    @Test
    fun directChatNeedsAnEstablishedExchange() =
        runTest {
            fakes.contacts.facts =
                listOf(
                    facts(CONTACT_ID, hasDirectConversation = true),
                    facts(OTHER_CONTACT_ID, hasDirectConversation = true)
                )
            fakes.identity.establishedPeers += OTHER_CONTACT_ID

            assertEquals(1, fakes.provisioner().provision().getOrThrow())

            assertEquals(listOf(OTHER_CONTACT_ID), fakes.gateway.created)
            assertEquals(listOf(OTHER_CONTACT_ID), fakes.outbox.enqueued.map { it.first })
        }

    @Test
    fun sharedActiveGroupKeepsAContactWithoutChatReachable() =
        runTest {
            fakes.contacts.facts = listOf(facts(CONTACT_ID, sharesActiveGroup = true))

            assertEquals(1, fakes.provisioner().provision().getOrThrow())

            assertEquals(listOf(CONTACT_ID), fakes.gateway.created)
        }

    private companion object {
        const val DAY = 24L * 60L * 60L * 1_000L
    }
}
