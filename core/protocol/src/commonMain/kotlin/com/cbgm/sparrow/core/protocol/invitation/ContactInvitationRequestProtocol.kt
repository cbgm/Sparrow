package com.cbgm.sparrow.core.protocol.invitation

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion

/** Verifies signed invitation wire fields before any peer reconciliation or feature state change. */
class ContactInvitationRequestProtocol(
    private val signatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: ContactInvitationPayloadEncoder
) {
    suspend fun createPacket(
        invitationId: String,
        displayName: String?,
        createdAtEpochMilliseconds: Long,
        expiresAtEpochMilliseconds: Long,
        profilePicture: ProfilePictureMetadata,
        inviteChallenge: ByteArray,
        encryptionPublicKey: ByteArray,
        signingKeyPair: LocalSigningKeyPair
    ): Result<ContactInvitePacket> = runCatching {
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        val packetId = "contact-invite-$invitationId"
        val signature = signatureCrypto.sign(
            payloadEncoder.encodeInvite(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                invitationId = invitationId,
                displayName = displayName,
                createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                profilePicture = profilePicture,
                inviteChallenge = inviteChallenge,
                encryptionPublicKey = encryptionPublicKey,
                signingPublicKey = signingKeyPair.publicKey
            ),
            signingKeyPair.privateKey
        ).getOrThrow()
        ContactInvitePacket(
            packetId = packetId,
            invitationId = invitationId,
            displayName = displayName,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            profilePicture = profilePicture,
            inviteChallenge = inviteChallenge.copyOf(),
            encryptionPublicKey = encryptionPublicKey.copyOf(),
            signingPublicKey = signingKeyPair.publicKey.copyOf(),
            signature = signature.copyOf()
        )
    }

    suspend fun verifyPacket(
        packet: ContactInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        check(packet.packetId == "contact-invite-${packet.invitationId}") {
            "Packet ID does not match the invitation transition"
        }
        signatureCrypto.verify(
            payloadEncoder.encodeInvite(
                packetId = packet.packetId,
                version = packet.version,
                invitationId = packet.invitationId,
                displayName = packet.displayName,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                profilePicture = packet.profilePicture,
                inviteChallenge = packet.inviteChallenge,
                encryptionPublicKey = packet.encryptionPublicKey,
                signingPublicKey = packet.signingPublicKey
            ),
            packet.signingPublicKey,
            packet.signature
        ).getOrThrow()
        require(packet.createdAtEpochMilliseconds <= receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS) {
            "Invitation was created too far in the future"
        }
        require(packet.expiresAtEpochMilliseconds > packet.createdAtEpochMilliseconds) {
            "Invitation expiry must be after its creation time"
        }
        require(packet.expiresAtEpochMilliseconds - packet.createdAtEpochMilliseconds <= INVITATION_LIFETIME_MILLISECONDS) {
            "Invitation lifetime exceeds the allowed maximum"
        }
        require(packet.expiresAtEpochMilliseconds > receivedAtEpochMilliseconds) {
            "Invitation has expired"
        }
    }

    private companion object {
        const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
        const val INVITATION_LIFETIME_MILLISECONDS = 24L * 60L * 60L * 1_000L
    }
}
