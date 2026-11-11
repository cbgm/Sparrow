package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvitationMailboxRenewalTest {
    private var now = 0L
    private var nodes: Result<List<NodeEndpoint>> = Result.success(listOf(NODE_A))
    private val store = MemoryStore()
    private val gateway = OpeningGateway()
    private val renewal =
        InvitationMailboxRenewal(
            nodeEndpointResolver =
                object : NodeEndpointResolver {
                    override suspend fun resolve(
                        localRoutingId: String,
                        forceRefresh: Boolean
                    ): Result<List<NodeEndpoint>> = nodes
                },
            localRoutingIdProvider =
                object : LocalRoutingIdProvider {
                    override suspend fun getLocalRoutingId(): Result<String> = Result.success("scrouting1_device")
                },
            mailboxGateway = gateway,
            store = store,
            now = { now }
        )

    @Test
    fun theFirstClaimOpensAnInvitationMailboxForThirtyDays() =
        runTest {
            val opened = renewal.current().getOrThrow()

            assertEquals(InvitationMailboxRenewal.CONTACT_ID, opened.contactId)
            assertEquals(30L * DAY, opened.deliveryRoute.expiresAtEpochMilliseconds)
            assertEquals(listOf(opened), store.mailboxes)
            assertEquals(opened, renewal.current().getOrThrow())
            assertEquals(1, gateway.opened)
        }

    @Test
    fun aWeekBeforeItExpiresANewOneTakesOverAndTheOldOneStaysReadableUntilItExpired() =
        runTest {
            val first = renewal.current().getOrThrow()
            now = 24L * DAY

            val second = renewal.current().getOrThrow()

            assertEquals(listOf(second, first), renewal.receiving().getOrThrow())
            now = 30L * DAY
            assertEquals(listOf(second), renewal.receiving().getOrThrow())
            renewal.current().getOrThrow()
            assertEquals(listOf(second), store.mailboxes)
        }

    @Test
    fun aMailboxWhoseNodeLeftTheDirectoryOrMovedIsReplaced() =
        runTest {
            val first = renewal.current().getOrThrow()
            nodes = Result.success(listOf(NODE_A.copy(mailboxRouteEndpoint = "https://node-a-moved.test")))

            val second = renewal.current().getOrThrow()

            assertTrue(second.deliveryRoute.routeId != first.deliveryRoute.routeId)
            assertEquals("https://node-a-moved.test", second.deliveryRoute.nodeEndpoint)
        }

    @Test
    fun withoutADirectoryAMailboxThatDoesNotExpireSoonIsKept() =
        runTest {
            val first = renewal.current().getOrThrow()
            nodes = Result.failure(IllegalStateException("offline"))

            assertEquals(first, renewal.current().getOrThrow())
            now = 24L * DAY
            assertTrue(renewal.current().isFailure)
        }

    private class MemoryStore : InvitationMailboxStore {
        var mailboxes: List<LocalMailboxCredential> = emptyList()

        override suspend fun all(): List<LocalMailboxCredential> = mailboxes

        override suspend fun replace(mailboxes: List<LocalMailboxCredential>) {
            this.mailboxes = mailboxes
        }
    }

    private class OpeningGateway : MailboxGateway {
        var opened = 0

        override suspend fun createInvitation(
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> {
            opened += 1
            return Result.success(
                LocalMailboxCredential(
                    contactId = InvitationMailboxRenewal.CONTACT_ID,
                    deliveryRoute =
                        MailboxDeliveryRoute(
                            routeId = "route-$opened",
                            nodeId = nodeId,
                            nodeEndpoint = routeEndpoint,
                            mailboxId = "mailbox-$opened",
                            sendCapability = "send-$opened",
                            sequence = 0L,
                            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                            identitySignature = byteArrayOf()
                        ),
                    accessEndpoint = accessEndpoint,
                    retrievalCapability = "retrieve-$opened"
                )
            )
        }

        override suspend fun create(
            contactId: String,
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            sequence: Long,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>> = error("Not used")

        override suspend fun acknowledge(
            credential: LocalMailboxCredential,
            envelopeId: String
        ): Result<Unit> = error("Not used")

        override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> = error("Not used")

        override suspend fun deliver(envelope: FederatedEnvelope): Result<Unit> = error("Not used")

        override suspend fun depositInvitation(envelope: FederatedEnvelope): Result<Unit> = error("Not used")

        override suspend fun ready(
            accessEndpoint: String,
            credentials: List<LocalMailboxCredential>
        ): Result<Set<String>> = error("Not used")
    }

    private companion object {
        const val DAY = 24L * 60L * 60L * 1_000L
        val NODE_A =
            NodeEndpoint(
                nodeId = "node-a",
                websocketUrl = "wss://node-a.test/ws",
                mailboxRouteEndpoint = "https://node-a.test",
                mailboxAccessEndpoint = "https://node-a.test"
            )
    }
}
