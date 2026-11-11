package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactTransportRoutingIdUseCase
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.InvalidInvitationLetterException
import com.cbgm.sparrow.feature.transport.mailbox.InvitationLetterContent
import com.cbgm.sparrow.feature.transport.mailbox.InvitationLetters
import com.cbgm.sparrow.feature.transport.mailbox.InvitationMailboxRenewal
import com.cbgm.sparrow.feature.transport.mailbox.InvitationMailboxStore
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class MailboxPendingSynchronizerTest {
    @Test
    fun failingEnvelopeOrMailboxDoesNotStopTheOthers() =
        runTest {
            val gateway =
                FakeMailboxGateway(
                    pending =
                        mapOf(
                            UNREACHABLE to null,
                            REACHABLE to listOf(envelope(FAILING), envelope(PROCESSED), envelope(REJECTED))
                        )
                )
            val synchronizer =
                MailboxPendingSynchronizer(
                    mailboxGateway = gateway,
                    mailboxRouteRepository = FakeMailboxRouteRepository(listOf(credential(UNREACHABLE), credential(REACHABLE))),
                    incomingEnvelopeProcessor = FakeProcessor,
                    resolveContactRoutingId = ResolveContactTransportRoutingIdUseCase(FakeContactTransport()),
                    invitationMailbox = invitationMailbox(),
                    invitationLetters = FakeLetters
                )

            assertEquals(1, synchronizer.synchronize().getOrThrow())
            // The failing envelope stays in its mailbox for the next sync; the rejected one is dropped.
            assertEquals(listOf(PROCESSED, REJECTED), gateway.acknowledged)
        }

    @Test
    fun retiringMailboxIsStillSynchronizedUntilItIsRevoked() =
        runTest {
            val gateway =
                FakeMailboxGateway(
                    pending = mapOf(REACHABLE to emptyList(), RETIRING to listOf(envelope(PROCESSED)))
                )
            val synchronizer =
                MailboxPendingSynchronizer(
                    mailboxGateway = gateway,
                    mailboxRouteRepository =
                        FakeMailboxRouteRepository(
                            credentials = listOf(credential(REACHABLE)),
                            retiring = listOf(credential(RETIRING), credential(REVOKING).copy(revocationPending = true))
                        ),
                    incomingEnvelopeProcessor = FakeProcessor,
                    resolveContactRoutingId = ResolveContactTransportRoutingIdUseCase(FakeContactTransport()),
                    invitationMailbox = invitationMailbox(),
                    invitationLetters = FakeLetters
                )

            assertEquals(1, synchronizer.synchronize().getOrThrow())
            assertEquals(listOf(PROCESSED), gateway.acknowledged)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun envelopesAreFetchedAsTheyArriveAndOnlyFromTheMailboxesThatGotSome() =
        runTest {
            val gateway =
                FakeMailboxGateway(
                    pending = mapOf(UNREACHABLE to null, REACHABLE to listOf(envelope(PROCESSED))),
                    readyOnce = setOf("mailbox-$REACHABLE")
                )
            val routes = FakeMailboxRouteRepository(listOf(credential(UNREACHABLE), credential(REACHABLE)))
            val synchronizer =
                MailboxPendingSynchronizer(
                    gateway,
                    routes,
                    FakeProcessor,
                    ResolveContactTransportRoutingIdUseCase(FakeContactTransport()),
                    invitationMailbox(),
                    FakeLetters
                )
            val listener = MailboxArrivalListener(gateway, routes, synchronizer, invitationMailbox())

            val listening = backgroundScope.launch { listener.listen() }
            advanceTimeBy(20_000.milliseconds)

            assertEquals(listOf(REACHABLE), gateway.fetched)
            assertEquals(listOf(PROCESSED), gateway.acknowledged)
            listening.cancel()
        }

    @Test
    fun theSenderIsTheMailboxesContactWhateverTheEnvelopeNames() =
        runTest {
            val gateway =
                FakeMailboxGateway(
                    pending = mapOf(REACHABLE to listOf(envelope(PROCESSED).copy(senderRoutingId = "someone-else"), envelope("hidden").withoutRoutingIds()))
                )
            val synchronizer =
                MailboxPendingSynchronizer(
                    mailboxGateway = gateway,
                    mailboxRouteRepository = FakeMailboxRouteRepository(listOf(credential(REACHABLE))),
                    incomingEnvelopeProcessor = FakeProcessor,
                    resolveContactRoutingId = ResolveContactTransportRoutingIdUseCase(FakeContactTransport()),
                    invitationMailbox = invitationMailbox(),
                    invitationLetters = FakeLetters
                )
            FakeProcessor.senders.clear()

            assertEquals(2, synchronizer.synchronize().getOrThrow())
            assertEquals(listOf("routing-$REACHABLE", "routing-$REACHABLE"), FakeProcessor.senders)
        }

    @Test
    fun aProvenLetterFromTheInvitationMailboxIsProcessedAsItsSender() =
        runTest {
            val gateway = FakeMailboxGateway(pending = mapOf(InvitationMailboxRenewal.CONTACT_ID to listOf(envelope(LETTER))))
            FakeProcessor.senders.clear()

            assertEquals(1, synchronizer(gateway, invitations = listOf(credential(InvitationMailboxRenewal.CONTACT_ID))).synchronize().getOrThrow())
            assertEquals(listOf("scphone1_alice"), FakeProcessor.senders)
            assertEquals(listOf(LETTER), gateway.acknowledged)
        }

    @Test
    fun aLetterThatCanNeverBeAcceptedIsDroppedAndOneThatCannotBeCheckedYetStays() =
        runTest {
            val gateway =
                FakeMailboxGateway(pending = mapOf(InvitationMailboxRenewal.CONTACT_ID to listOf(envelope(SPOOFED_LETTER), envelope(UNCHECKED_LETTER))))
            FakeProcessor.senders.clear()

            assertEquals(0, synchronizer(gateway, invitations = listOf(credential(InvitationMailboxRenewal.CONTACT_ID))).synchronize().getOrThrow())
            assertEquals(emptyList(), FakeProcessor.senders)
            assertEquals(listOf(SPOOFED_LETTER), gateway.acknowledged)
        }

    private fun synchronizer(
        gateway: FakeMailboxGateway,
        invitations: List<LocalMailboxCredential>
    ) = MailboxPendingSynchronizer(
        mailboxGateway = gateway,
        mailboxRouteRepository = FakeMailboxRouteRepository(emptyList()),
        incomingEnvelopeProcessor = FakeProcessor,
        resolveContactRoutingId = ResolveContactTransportRoutingIdUseCase(FakeContactTransport()),
        invitationMailbox = invitationMailbox(invitations),
        invitationLetters = FakeLetters
    )

    /** Only reads its store here: no node is asked. */
    private fun invitationMailbox(invitations: List<LocalMailboxCredential> = emptyList()) =
        InvitationMailboxRenewal(
            nodeEndpointResolver = object : NodeEndpointResolver {
                override suspend fun resolve(localRoutingId: String, forceRefresh: Boolean): Result<List<NodeEndpoint>> = error("Not used")
            },
            localRoutingIdProvider = object : LocalRoutingIdProvider {
                override suspend fun getLocalRoutingId(): Result<String> = error("Not used")
            },
            mailboxGateway = FakeMailboxGateway(emptyMap()),
            store =
                object : InvitationMailboxStore {
                    override suspend fun all(): List<LocalMailboxCredential> = invitations

                    override suspend fun replace(mailboxes: List<LocalMailboxCredential>) = error("Not used")
                },
            now = { 0L }
        )

    /** The sender of [LETTER] is proven, [SPOOFED_LETTER] claims a number that is not its own, [UNCHECKED_LETTER] cannot be checked now. */
    private object FakeLetters : InvitationLetters {
        override suspend fun send(
            senderRoutingId: String,
            recipientRoutingId: String,
            payload: String,
            envelopeId: String,
            createdAtEpochMilliseconds: Long
        ): Result<Long> = error("Not used")

        override suspend fun open(envelope: FederatedEnvelope): Result<InvitationLetterContent> =
            when (envelope.envelopeId) {
                LETTER -> Result.success(InvitationLetterContent("scphone1_alice", "invite"))
                SPOOFED_LETTER -> Result.failure(InvalidInvitationLetterException("The sender's key does not prove its routing ID"))
                else -> Result.failure(IllegalStateException("presence directory unreachable"))
            }
    }

    private object FakeProcessor : IncomingEnvelopeProcessor {
        val senders = mutableListOf<String>()

        override suspend fun process(
            envelopeId: String,
            senderRoutingId: String,
            encodedTransportPayload: String
        ): Result<IncomingEnvelopeProcessingResult> =
            when (envelopeId.also { senders += senderRoutingId }) {
                FAILING -> Result.failure(IllegalStateException("database is busy"))
                REJECTED -> Result.success(IncomingEnvelopeProcessingResult.Rejected)
                else -> Result.success(IncomingEnvelopeProcessingResult.Processed)
            }
    }

    private class FakeMailboxGateway(
        private val pending: Map<String, List<FederatedEnvelope>?>,
        private var readyOnce: Set<String> = emptySet()
    ) : MailboxGateway {
        val acknowledged = mutableListOf<String>()
        val fetched = mutableListOf<String>()

        override suspend fun create(
            contactId: String,
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            sequence: Long,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>> =
            pending[credential.contactId]
                ?.let { Result.success(it).also { fetched += credential.contactId } }
                ?: Result.failure(IllegalStateException("node unreachable"))

        override suspend fun acknowledge(credential: LocalMailboxCredential, envelopeId: String): Result<Unit> {
            acknowledged += envelopeId
            return Result.success(Unit)
        }

        override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> = error("Not used")

        override suspend fun deliver(envelope: FederatedEnvelope): Result<Unit> = error("Not used")

        override suspend fun createInvitation(
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun depositInvitation(envelope: FederatedEnvelope): Result<Unit> = error("Not used")

        /** Like the node: answers at once when mailboxes hold envelopes, otherwise after its wait. */
        override suspend fun ready(
            accessEndpoint: String,
            credentials: List<LocalMailboxCredential>
        ): Result<Set<String>> {
            val ready = readyOnce.also { readyOnce = emptySet() }
            if (ready.isEmpty()) delay(8_000.milliseconds)
            return Result.success(ready)
        }
    }

    private class FakeMailboxRouteRepository(
        private val credentials: List<LocalMailboxCredential>,
        private val retiring: List<LocalMailboxCredential> = emptyList()
    ) : MailboxRouteRepository {
        override suspend fun localForContact(contactId: String) = error("Not used")

        override suspend fun remoteForRecipientRoutingId(routingId: String) = error("Not used")

        override suspend fun remoteForContact(contactId: String) = error("Not used")

        override suspend fun allLocal() = Result.success(credentials)

        override suspend fun saveLocal(credential: LocalMailboxCredential) = error("Not used")

        override suspend fun saveRemote(contactId: String, route: MailboxDeliveryRoute) = error("Not used")

        override suspend fun markLocalRevocationPending(contactId: String) = error("Not used")

        override suspend fun deleteLocal(contactId: String) = error("Not used")

        override suspend fun deleteRemote(contactId: String) = error("Not used")

        override suspend fun deleteAllRemote() = error("Not used")

        override suspend fun allRetiring() = Result.success(retiring)

        override suspend fun retireLocal(retiring: LocalMailboxCredential, replacement: LocalMailboxCredential) =
            error("Not used")

        override suspend fun saveRetiring(credential: LocalMailboxCredential) = error("Not used")

        override suspend fun markRetiringRevocationPending(contactId: String) = error("Not used")

        override suspend fun deleteRetiring(routeId: String) = error("Not used")
    }

    private companion object {
        const val UNREACHABLE = "contact-unreachable"
        const val REACHABLE = "contact-reachable"
        const val RETIRING = "contact-retiring"
        const val REVOKING = "contact-revoking"
        const val FAILING = "envelope-failing"
        const val PROCESSED = "envelope-processed"
        const val REJECTED = "envelope-rejected"
        const val LETTER = "letter"
        const val SPOOFED_LETTER = "letter-spoofed"
        const val UNCHECKED_LETTER = "letter-unchecked"

        fun envelope(id: String) =
            FederatedEnvelope(
                envelopeId = id,
                senderRoutingId = "sender",
                recipientDeviceRoutingId = "recipient",
                encryptedPayload = "payload",
                createdAtEpochMilliseconds = 0,
                expiresAtEpochMilliseconds = Long.MAX_VALUE
            )

        fun credential(contactId: String) =
            LocalMailboxCredential(
                contactId = contactId,
                deliveryRoute =
                    MailboxDeliveryRoute(
                        routeId = "route-$contactId",
                        nodeId = "node-a",
                        nodeEndpoint = "http://mailbox",
                        mailboxId = "mailbox-$contactId",
                        sendCapability = "send-capability",
                        sequence = 0,
                        expiresAtEpochMilliseconds = Long.MAX_VALUE,
                        identitySignature = byteArrayOf(1)
                    ),
                accessEndpoint = "http://mailbox",
                retrievalCapability = "retrieval-capability"
            )
    }
}
