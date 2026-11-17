package com.cbgm.sparrow.feature.transport.sender

import com.cbgm.sparrow.feature.transport.config.TransportConfig
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.InvalidInvitationLetterException
import com.cbgm.sparrow.feature.transport.mailbox.InvitationRouteMissingException
import com.cbgm.sparrow.feature.transport.mailbox.InvitationTestWorld
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.presence.PhoneAliasBinding
import com.cbgm.sparrow.feature.transport.routing.LocalBootstrapRoutingIdProvider
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class MailboxOutgoingWireSenderTest {
    private val world = InvitationTestWorld()
    private val bob = world.device("bob")
    private val mailboxes = RecordingMailboxGateway()

    @Test
    fun aContactWithAMailboxGetsTheEnvelopePutIntoIt() =
        runTest {
            val acceptance = sender(FakeMailboxRouteRepository(ROUTE)).sendWithAcceptance("recipient-routing-id", "encoded-payload").getOrThrow()

            val delivered = mailboxes.delivered.single()
            assertEquals(ROUTE, delivered.mailboxRoute)
            assertEquals("encoded-payload", delivered.encryptedPayload)
            // The mailbox and the nodes on the way do not learn who writes to whom.
            assertEquals(FederatedEnvelope.HIDDEN_ROUTING_ID, delivered.senderRoutingId)
            assertEquals(FederatedEnvelope.HIDDEN_ROUTING_ID, delivered.recipientDeviceRoutingId)
            assertEquals(delivered.expiresAtEpochMilliseconds, acceptance.expiresAtEpochMilliseconds)
            assertTrue(world.mailboxes.deposited.isEmpty())
        }

    @Test
    fun theContactsRouteIsFoundByTheContactEvenWhenTheRoutingTableDoesNotKnowTheAddress() =
        runTest {
            sender(FakeMailboxRouteRepository(ROUTE, knowsAddress = false))
                .sendWithAcceptance("scrouting1_contact", "encoded-payload", recipientContactId = "contact")
                .getOrThrow()

            assertEquals(ROUTE, mailboxes.delivered.single().mailboxRoute)
        }

    @Test
    fun anUnreachableMailboxFailsTheSendSoTheOutboxRetriesIt() =
        runTest {
            mailboxes.fails = true

            assertTrue(sender(FakeMailboxRouteRepository(ROUTE)).send("recipient-routing-id", "encoded-payload").isFailure)
            assertTrue(world.mailboxes.deposited.isEmpty())
        }

    @Test
    fun withoutAMailboxRouteThePacketGoesIntoTheRecipientsInvitationMailbox() =
        runTest {
            world.publish(bob)

            val acceptance = sender().sendWithAcceptance(bob.routingId, "encoded-payload").getOrThrow()

            val envelope = world.mailboxes.deposited.single()
            assertEquals(bob.route, envelope.mailboxRoute)
            assertEquals(envelope.expiresAtEpochMilliseconds, acceptance.expiresAtEpochMilliseconds)
            assertTrue(mailboxes.delivered.isEmpty())
        }

    @Test
    fun aFirstInvitationIsSentAsTheOwnPhoneAliasOnceItIsBound() =
        runTest {
            world.publish(bob)
            val waitedFor = mutableListOf<String>()

            assertTrue(sender(binding = { alias, _ -> waitedFor += alias }).send(bob.alias, "encoded-payload").isSuccess)

            assertEquals(listOf("local-bootstrap-routing-id"), waitedFor)
            // The test sender's alias is not bound to its key in this world, so Bob refuses it: the alias was sent.
            assertIs<InvalidInvitationLetterException>(world.letters(bob).open(world.mailboxes.deposited.single()).exceptionOrNull())
        }

    @Test
    fun aFirstInvitationWaitsInTheOutboxWhileTheOwnPhoneAliasIsNotBound() =
        runTest {
            world.publish(bob)
            val unbound = PhoneAliasBinding { _, timeout -> withTimeout(timeout.milliseconds) { delay(timeout * 2) } }

            val failure = sender(binding = unbound).send(bob.alias, "encoded-payload").exceptionOrNull()

            assertIs<IllegalStateException>(failure)
            assertIs<TimeoutCancellationException>(failure.cause)
            assertTrue(world.mailboxes.deposited.isEmpty())
        }

    @Test
    fun aRecipientWithoutInvitationMailboxFailsTheSend() =
        runTest {
            assertIs<InvitationRouteMissingException>(sender().send("scrouting1_nobody", "encoded-payload").exceptionOrNull())
            assertTrue(world.mailboxes.deposited.isEmpty())
        }

    @Test
    fun localRoutingIdFailurePreventsTheSend() =
        runTest {
            world.publish(bob)
            val expectedError = IllegalStateException("local routing ID unavailable")

            val result = sender(localRoutingId = Result.failure(expectedError)).send(bob.routingId, "encoded-payload")

            assertSame(expectedError, result.exceptionOrNull())
            assertTrue(world.mailboxes.deposited.isEmpty())
        }

    private fun sender(
        routes: MailboxRouteRepository = FakeMailboxRouteRepository(route = null),
        binding: PhoneAliasBinding = PhoneAliasBinding { _, _ -> },
        localRoutingId: Result<String> = Result.success("local-routing-id")
    ) = MailboxOutgoingWireSender(
        localRoutingIdProvider =
            object : LocalRoutingIdProvider {
                override suspend fun getLocalRoutingId(): Result<String> = localRoutingId
            },
        localBootstrapRoutingIdProvider =
            object : LocalBootstrapRoutingIdProvider {
                override suspend fun getLocalBootstrapRoutingId(): Result<String> = Result.success("local-bootstrap-routing-id")
            },
        transportConfig = TransportConfig(acknowledgementTimeoutMilliseconds = 2_500L),
        mailboxRouteRepository = routes,
        mailboxGateway = mailboxes,
        invitationLetters = world.letters(world.device("alice")),
        aliasBinding = binding
    )

    private class RecordingMailboxGateway : MailboxGateway {
        var fails = false
        val delivered = mutableListOf<FederatedEnvelope>()

        override suspend fun deliver(envelope: FederatedEnvelope): Result<Unit> =
            if (fails) {
                Result.failure(IllegalStateException("Mailbox delivery failed with 502"))
            } else {
                delivered += envelope
                Result.success(Unit)
            }

        override suspend fun create(
            contactId: String,
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            sequence: Long,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun createInvitation(
            nodeId: String,
            routeEndpoint: String,
            accessEndpoint: String,
            expiresAtEpochMilliseconds: Long
        ): Result<LocalMailboxCredential> = error("Not used")

        override suspend fun depositInvitation(envelope: FederatedEnvelope): Result<Unit> = error("Not used")

        override suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>> = error("Not used")

        override suspend fun acknowledge(
            credential: LocalMailboxCredential,
            envelopeId: String
        ): Result<Unit> = error("Not used")

        override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> = error("Not used")

        override suspend fun ready(
            accessEndpoint: String,
            credentials: List<LocalMailboxCredential>
        ): Result<Set<String>> = error("Not used")
    }

    /** Like the app's table: with [knowsAddress] false, only the lookup by contact finds the route. */
    private class FakeMailboxRouteRepository(
        private val route: MailboxDeliveryRoute?,
        private val knowsAddress: Boolean = true
    ) : MailboxRouteRepository {
        override suspend fun localForContact(contactId: String) = Result.success<LocalMailboxCredential?>(null)

        override suspend fun remoteForRecipientRoutingId(routingId: String) = Result.success(route.takeIf { knowsAddress })

        override suspend fun remoteForContact(contactId: String) = Result.success(route)

        override suspend fun allLocal() = Result.success(emptyList<LocalMailboxCredential>())

        override suspend fun saveLocal(credential: LocalMailboxCredential) = Result.success(Unit)

        override suspend fun saveRemote(
            contactId: String,
            route: MailboxDeliveryRoute
        ) = Result.success(Unit)

        override suspend fun markLocalRevocationPending(contactId: String) = Result.success(Unit)

        override suspend fun deleteLocal(contactId: String) = Result.success(Unit)

        override suspend fun deleteRemote(contactId: String) = Result.success(Unit)

        override suspend fun deleteAllRemote() = Result.success(Unit)

        override suspend fun allRetiring() = Result.success(emptyList<LocalMailboxCredential>())

        override suspend fun retireLocal(
            retiring: LocalMailboxCredential,
            replacement: LocalMailboxCredential
        ) = Result.success(Unit)

        override suspend fun saveRetiring(credential: LocalMailboxCredential) = Result.success(Unit)

        override suspend fun markRetiringRevocationPending(contactId: String) = Result.success(Unit)

        override suspend fun deleteRetiring(routeId: String) = Result.success(Unit)
    }

    private companion object {
        val ROUTE =
            MailboxDeliveryRoute(
                routeId = "route-1",
                nodeId = "node-b",
                nodeEndpoint = "https://node-b.example",
                mailboxId = "mailbox-1",
                sendCapability = "send-capability",
                sequence = 4L,
                expiresAtEpochMilliseconds = Long.MAX_VALUE,
                identitySignature = byteArrayOf(1, 2, 3)
            )
    }
}
