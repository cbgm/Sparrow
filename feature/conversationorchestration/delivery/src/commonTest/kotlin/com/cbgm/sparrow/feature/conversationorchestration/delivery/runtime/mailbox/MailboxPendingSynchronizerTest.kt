package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.sparrow.feature.messaging.runtime.incoming.IncomingEnvelopeProcessor
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.mailbox.MailboxWakeTicket
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
                    incomingEnvelopeProcessor = FakeProcessor
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
                    incomingEnvelopeProcessor = FakeProcessor
                )

            assertEquals(1, synchronizer.synchronize().getOrThrow())
            assertEquals(listOf(PROCESSED), gateway.acknowledged)
        }

    private object FakeProcessor : IncomingEnvelopeProcessor {
        override suspend fun process(
            envelopeId: String,
            senderRoutingId: String,
            encodedTransportPayload: String
        ): Result<IncomingEnvelopeProcessingResult> =
            when (envelopeId) {
                FAILING -> Result.failure(IllegalStateException("database is busy"))
                REJECTED -> Result.success(IncomingEnvelopeProcessingResult.Rejected)
                else -> Result.success(IncomingEnvelopeProcessingResult.Processed)
            }
    }

    private class FakeMailboxGateway(
        private val pending: Map<String, List<FederatedEnvelope>?>
    ) : MailboxGateway {
        val acknowledged = mutableListOf<String>()

        override suspend fun create(
            contactId: String,
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            sequence: Long,
            expiresAtEpochMilliseconds: Long,
            wakeTicket: MailboxWakeTicket
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>> =
            pending[credential.contactId]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("node unreachable"))

        override suspend fun acknowledge(credential: LocalMailboxCredential, envelopeId: String): Result<Unit> {
            acknowledged += envelopeId
            return Result.success(Unit)
        }

        override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> = error("Not used")
    }

    private class FakeMailboxRouteRepository(
        private val credentials: List<LocalMailboxCredential>,
        private val retiring: List<LocalMailboxCredential> = emptyList()
    ) : MailboxRouteRepository {
        override suspend fun localForContact(contactId: String) = error("Not used")

        override suspend fun remoteForRecipientRoutingId(routingId: String) = error("Not used")

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
