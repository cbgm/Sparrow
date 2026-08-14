package com.cbgm.sparrow.core.protocol.codec

import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ContactInvitationPacketCodecTest {
    private val codec = KotlinxPacketCodec(json = createProtocolJson())

    @Test
    fun contactInviteRoundTrip() {
        val original =
            ContactInvitePacket(
                packetId = "contact-invite-invitation-1",
                invitationId = "invitation-1",
                displayName = "Alice",
                createdAtEpochMilliseconds = 100L,
                expiresAtEpochMilliseconds = 200L,
                inviteChallenge = challenge(1),
                encryptionPublicKey = key(2),
                signingPublicKey = key(3),
                signature = signature(4)
            )

        assertEquals(original, roundTrip<ContactInvitePacket>(original))
    }

    @Test
    fun contactInviteAcceptedRoundTrip() {
        val original =
            ContactInviteAcceptedPacket(
                packetId = "contact-invite-accepted-invitation-1",
                invitationId = "invitation-1",
                acceptedAtEpochMilliseconds = 150L,
                inviteChallenge = challenge(1),
                responseChallenge = challenge(2),
                inviterEncryptionPublicKey = key(3),
                inviterSigningPublicKey = key(4),
                responderEncryptionPublicKey = key(5),
                responderSigningPublicKey = key(6),
                signature = signature(7)
            )

        assertEquals(original, roundTrip<ContactInviteAcceptedPacket>(original))
    }

    @Test
    fun contactReadyRoundTrip() {
        val original =
            ContactReadyPacket(
                packetId = "contact-ready-invitation-1",
                invitationId = "invitation-1",
                readyAtEpochMilliseconds = 175L,
                responseChallenge = challenge(2),
                acceptedResponderEncryptionPublicKey = key(3),
                acceptedResponderSigningPublicKey = key(4),
                senderEncryptionPublicKey = key(5),
                senderSigningPublicKey = key(6),
                signature = signature(7)
            )

        assertEquals(original, roundTrip<ContactReadyPacket>(original))
    }

    @Test
    fun contactVerificationReceiptRoundTrip() {
        val original =
            ContactVerificationReceiptPacket(
                packetId = "contact-verification-receipt-receipt-1",
                receiptId = "receipt-1",
                verifiedAtEpochMilliseconds = 180L,
                senderEncryptionPublicKey = key(1),
                senderSigningPublicKey = key(2),
                verifiedEncryptionPublicKey = key(3),
                verifiedSigningPublicKey = key(4),
                signature = signature(5)
            )

        assertEquals(original, roundTrip<ContactVerificationReceiptPacket>(original))
    }

    @Test
    fun contactInviteDeclinedRoundTrip() {
        val original =
            ContactInviteDeclinedPacket(
                packetId = "contact-invite-declined-invitation-1",
                invitationId = "invitation-1",
                declinedAtEpochMilliseconds = 150L,
                inviteChallenge = challenge(1),
                declinerSigningPublicKey = key(2),
                signature = signature(3)
            )

        assertEquals(original, roundTrip<ContactInviteDeclinedPacket>(original))
    }

    @Test
    fun directChatAuthorizationRevokedRoundTrip() {
        val original =
            DirectChatAuthorizationRevokedPacket(
                packetId = "direct-chat-authorization-revoked-invitation-1",
                invitationId = "invitation-1",
                revokedAtEpochMilliseconds = 160L,
                inviteChallenge = challenge(1),
                revokerSigningPublicKey = key(2),
                signature = signature(3)
            )

        assertEquals(original, roundTrip<DirectChatAuthorizationRevokedPacket>(original))
    }

    private inline fun <reified T : Any> roundTrip(packet: com.cbgm.sparrow.core.protocol.packet.SparrowPacket): T =
        assertIs<T>(
            codec.decode(codec.encode(packet).getOrThrow()).getOrThrow()
        )

    private fun challenge(value: Byte): ByteArray = ByteArray(size = 32) { value }

    private fun key(value: Byte): ByteArray = ByteArray(size = 32) { value }

    private fun signature(value: Byte): ByteArray = ByteArray(size = 64) { value }
}
