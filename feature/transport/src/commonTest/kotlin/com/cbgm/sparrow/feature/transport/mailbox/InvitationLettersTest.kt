package com.cbgm.sparrow.feature.transport.mailbox

import com.cbgm.sparrow.core.crypto.transport.DefaultTransportPayloadCodec
import com.cbgm.sparrow.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.feature.transport.gateway.codec.createGatewayJson
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InvitationLettersTest {
    private val world = InvitationTestWorld()
    private val alice = world.device("alice")
    private val bob = world.device("bob")

    @Test
    fun aFirstInvitationReachesThePhoneAliasAndItsSenderIsProven() =
        runTest {
            world.publish(alice)
            world.publish(bob)

            val expiresAt = world.letters(alice).send(alice.alias, bob.alias, "invite", "envelope-1", 1_000L).getOrThrow()

            val envelope = world.mailboxes.deposited.single()
            assertEquals(bob.route, envelope.mailboxRoute)
            assertEquals(1_000L + 7L * 24L * 60L * 60L * 1_000L, expiresAt)
            // Neither the nodes on the way nor the mailbox learn who writes to whom, or what.
            assertEquals(FederatedEnvelope.HIDDEN_ROUTING_ID, envelope.senderRoutingId)
            assertEquals(FederatedEnvelope.HIDDEN_ROUTING_ID, envelope.recipientDeviceRoutingId)
            assertFalse("invite" in envelope.encryptedPayload)
            assertEquals(InvitationLetterContent(alice.alias, "invite"), world.letters(bob).open(envelope).getOrThrow())
        }

    @Test
    fun aDeviceRoutingIdIsProvenByTheKeyAlone() =
        runTest {
            world.publish(bob)

            world.letters(alice).send(alice.routingId, bob.routingId, "accepted", "envelope-1", 1_000L).getOrThrow()

            assertEquals(InvitationLetterContent(alice.routingId, "accepted"), world.letters(bob).open(world.mailboxes.deposited.single()).getOrThrow())
        }

    @Test
    fun nobodyCanClaimAnotherPersonsPhoneNumber() =
        runTest {
            val mallory = world.device("mallory")
            world.publish(alice)
            world.publish(bob)

            world.letters(mallory).send(alice.alias, bob.alias, "fake invite", "envelope-1", 1_000L).getOrThrow()

            assertIs<InvalidInvitationLetterException>(world.letters(bob).open(world.mailboxes.deposited.single()).exceptionOrNull())
        }

    @Test
    fun aRoutingIdOfAnotherDeviceIsRefused() =
        runTest {
            world.publish(bob)

            world.letters(world.device("mallory")).send(alice.routingId, bob.routingId, "fake", "envelope-1", 1_000L).getOrThrow()

            assertIs<InvalidInvitationLetterException>(world.letters(bob).open(world.mailboxes.deposited.single()).exceptionOrNull())
        }

    @Test
    fun aLetterForAnotherDeviceIsRefused() =
        runTest {
            world.publish(alice)
            world.publish(bob)
            world.letters(alice).send(alice.routingId, bob.routingId, "for bob", "envelope-1", 1_000L).getOrThrow()

            assertIs<InvalidInvitationLetterException>(world.letters(world.device("carol")).open(world.mailboxes.deposited.single()).exceptionOrNull())
        }

    @Test
    fun aLetterNamingAnotherRecipientIsRefused() =
        runTest {
            val carol = world.device("carol")
            world.publish(bob)
            // A Control Plane that hands out Bob's mailbox for Carol: the letter names Carol, so Bob refuses it.
            world.presence[carol.routingId] = world.presence.getValue(bob.routingId)

            world.letters(alice).send(alice.routingId, carol.routingId, "for carol", "envelope-1", 1_000L).getOrThrow()

            assertIs<InvalidInvitationLetterException>(world.letters(bob).open(world.mailboxes.deposited.single()).exceptionOrNull())
        }

    @Test
    fun anUnsealedOrUnreadablePayloadIsRefused() =
        runTest {
            val codec = DefaultTransportPayloadCodec()
            val plaintext = codec.encode(EncryptedTransportPayload(1, TransportEncryptionMode.PLAINTEXT, "invite".encodeToByteArray()))

            assertIs<InvalidInvitationLetterException>(world.letters(bob).open(envelope(plaintext)).exceptionOrNull())
            assertIs<InvalidInvitationLetterException>(world.letters(bob).open(envelope("not a payload")).exceptionOrNull())
        }

    @Test
    fun withoutAnInvitationRouteNothingIsSent() =
        runTest {
            val failure = world.letters(alice).send(alice.alias, bob.alias, "invite", "envelope-1", 1_000L).exceptionOrNull()

            assertIs<InvitationRouteMissingException>(failure)
            assertTrue(world.mailboxes.deposited.isEmpty())
        }

    @Test
    fun aPhoneAliasThatCannotBeCheckedNowStaysForLater() =
        runTest {
            world.publish(alice)
            world.publish(bob)
            world.letters(alice).send(alice.alias, bob.alias, "invite", "envelope-1", 1_000L).getOrThrow()
            world.presenceFails = true

            val failure = world.letters(bob).open(world.mailboxes.deposited.single()).exceptionOrNull()

            assertTrue(failure != null && failure !is InvalidInvitationLetterException)
        }

    @Test
    fun theLetterIsSignedOverEveryField() =
        runTest {
            world.publish(bob)
            world.letters(alice).send(alice.routingId, bob.routingId, "accepted", "envelope-1", 1_000L).getOrThrow()
            val json = createGatewayJson()
            val codec = DefaultTransportPayloadCodec()
            val sealed = codec.decode(world.mailboxes.deposited.single().encryptedPayload).getOrThrow()
            val prefix = bob.key + byteArrayOf(0)
            val letter = json.decodeFromString<InvitationLetter>(sealed.payload.copyOfRange(prefix.size, sealed.payload.size).decodeToString())
            val changed =
                InvitationLetter(letter.senderRoutingId, letter.recipientRoutingId, letter.signingPublicKey, "changed", letter.createdAtEpochMilliseconds, letter.signature)
            val resealed = codec.encode(EncryptedTransportPayload(1, TransportEncryptionMode.SEALED_BOX, prefix + json.encodeToString(changed).encodeToByteArray()))

            assertIs<InvalidInvitationLetterException>(world.letters(bob).open(envelope(resealed)).exceptionOrNull())
        }

    private fun envelope(payload: String) =
        FederatedEnvelope(
            envelopeId = "envelope-x",
            senderRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
            recipientDeviceRoutingId = FederatedEnvelope.HIDDEN_ROUTING_ID,
            mailboxRoute = bob.route,
            encryptedPayload = payload,
            createdAtEpochMilliseconds = 1_000L,
            expiresAtEpochMilliseconds = 2_000L
        )
}
