package com.cbgm.sparrow.core.protocol.invitation

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion

/** Only signed contact-invitation decline wire data; never modifies invitation or identity state. */
class ContactInvitationDeclineProtocol(
    private val signatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: ContactInvitationPayloadEncoder
) {
    suspend fun createPacket(
        exchangeId: String,
        inviteChallenge: ByteArray,
        declinedAtEpochMilliseconds: Long,
        signingKeyPair: LocalSigningKeyPair
    ): Result<ContactInviteDeclinedPacket> = runCatching {
        require(exchangeId.isNotBlank()) { "Exchange ID must not be blank" }
        val packetId = "contact-invite-declined-$exchangeId"
        val signature = signatureCrypto.sign(
            payloadEncoder.encodeDeclined(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                invitationId = exchangeId,
                declinedAtEpochMilliseconds = declinedAtEpochMilliseconds,
                inviteChallenge = inviteChallenge,
                declinerSigningPublicKey = signingKeyPair.publicKey
            ),
            signingKeyPair.privateKey
        ).getOrThrow()
        ContactInviteDeclinedPacket(
            packetId = packetId,
            invitationId = exchangeId,
            declinedAtEpochMilliseconds = declinedAtEpochMilliseconds,
            inviteChallenge = inviteChallenge.copyOf(),
            declinerSigningPublicKey = signingKeyPair.publicKey.copyOf(),
            signature = signature.copyOf()
        )
    }

    /** Signature and timestamp checks are independent of stored Identity/Invite state. */
    suspend fun verifyPacket(
        packet: ContactInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        check(packet.packetId == "contact-invite-declined-${packet.invitationId}") {
            "Packet ID does not match the invitation transition"
        }
        signatureCrypto.verify(
            payloadEncoder.encodeDeclined(
                packetId = packet.packetId,
                version = packet.version,
                invitationId = packet.invitationId,
                declinedAtEpochMilliseconds = packet.declinedAtEpochMilliseconds,
                inviteChallenge = packet.inviteChallenge,
                declinerSigningPublicKey = packet.declinerSigningPublicKey
            ),
            packet.declinerSigningPublicKey,
            packet.signature
        ).getOrThrow()
        require(packet.declinedAtEpochMilliseconds <= receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS) {
            "Decline response was created too far in the future"
        }
    }

    private companion object {
        const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
    }
}
