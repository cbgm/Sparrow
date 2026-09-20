package com.cbgm.sparrow.core.protocol.invitation

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion

/** Signed acceptance and ready wire packets only; the exchange state belongs to Identity. */
class ContactInvitationHandshakeProtocol(
    private val signatureCrypto: DetachedSignatureCrypto,
    private val payloadEncoder: ContactInvitationPayloadEncoder
) {
    suspend fun verifyAccepted(
        packet: ContactInviteAcceptedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        check(packet.packetId == "contact-invite-accepted-${packet.invitationId}") {
            "Packet ID does not match the invitation transition"
        }
        signatureCrypto.verify(
            payloadEncoder.encodeAccepted(
                packetId = packet.packetId,
                version = packet.version,
                invitationId = packet.invitationId,
                acceptedAtEpochMilliseconds = packet.acceptedAtEpochMilliseconds,
                profilePicture = packet.profilePicture,
                inviteChallenge = packet.inviteChallenge,
                responseChallenge = packet.responseChallenge,
                inviterEncryptionPublicKey = packet.inviterEncryptionPublicKey,
                inviterSigningPublicKey = packet.inviterSigningPublicKey,
                responderEncryptionPublicKey = packet.responderEncryptionPublicKey,
                responderSigningPublicKey = packet.responderSigningPublicKey
            ),
            packet.responderSigningPublicKey,
            packet.signature
        ).getOrThrow()
        require(packet.acceptedAtEpochMilliseconds <= receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS) {
            "Acceptance was created too far in the future"
        }
    }

    suspend fun verifyReady(
        packet: ContactReadyPacket,
        remoteSigningPublicKey: ByteArray,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        check(packet.packetId == "contact-ready-${packet.invitationId}") {
            "Packet ID does not match the invitation transition"
        }
        signatureCrypto.verify(
            payloadEncoder.encodeReady(
                packetId = packet.packetId,
                version = packet.version,
                invitationId = packet.invitationId,
                readyAtEpochMilliseconds = packet.readyAtEpochMilliseconds,
                responseChallenge = packet.responseChallenge,
                acceptedResponderEncryptionPublicKey = packet.acceptedResponderEncryptionPublicKey,
                acceptedResponderSigningPublicKey = packet.acceptedResponderSigningPublicKey,
                senderEncryptionPublicKey = packet.senderEncryptionPublicKey,
                senderSigningPublicKey = packet.senderSigningPublicKey
            ),
            remoteSigningPublicKey,
            packet.signature
        ).getOrThrow()
        require(packet.readyAtEpochMilliseconds <= receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS) {
            "Ready confirmation was created too far in the future"
        }
    }

    suspend fun createAccepted(
        invitationId: String,
        acceptedAtEpochMilliseconds: Long,
        profilePicture: ProfilePictureMetadata,
        inviteChallenge: ByteArray,
        responseChallenge: ByteArray,
        inviterEncryptionPublicKey: ByteArray,
        inviterSigningPublicKey: ByteArray,
        responderEncryptionPublicKey: ByteArray,
        signingKeyPair: LocalSigningKeyPair
    ): Result<ContactInviteAcceptedPacket> = runCatching {
        val packetId = "contact-invite-accepted-$invitationId"
        val signature = signatureCrypto.sign(
            payloadEncoder.encodeAccepted(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                invitationId = invitationId,
                acceptedAtEpochMilliseconds = acceptedAtEpochMilliseconds,
                profilePicture = profilePicture,
                inviteChallenge = inviteChallenge,
                responseChallenge = responseChallenge,
                inviterEncryptionPublicKey = inviterEncryptionPublicKey,
                inviterSigningPublicKey = inviterSigningPublicKey,
                responderEncryptionPublicKey = responderEncryptionPublicKey,
                responderSigningPublicKey = signingKeyPair.publicKey
            ),
            signingKeyPair.privateKey
        ).getOrThrow()
        ContactInviteAcceptedPacket(
            packetId = packetId,
            invitationId = invitationId,
            acceptedAtEpochMilliseconds = acceptedAtEpochMilliseconds,
            profilePicture = profilePicture,
            inviteChallenge = inviteChallenge.copyOf(),
            responseChallenge = responseChallenge.copyOf(),
            inviterEncryptionPublicKey = inviterEncryptionPublicKey.copyOf(),
            inviterSigningPublicKey = inviterSigningPublicKey.copyOf(),
            responderEncryptionPublicKey = responderEncryptionPublicKey.copyOf(),
            responderSigningPublicKey = signingKeyPair.publicKey.copyOf(),
            signature = signature.copyOf()
        )
    }

    suspend fun createReady(
        invitationId: String,
        readyAtEpochMilliseconds: Long,
        responseChallenge: ByteArray,
        acceptedResponderEncryptionPublicKey: ByteArray,
        acceptedResponderSigningPublicKey: ByteArray,
        senderEncryptionPublicKey: ByteArray,
        signingKeyPair: LocalSigningKeyPair
    ): Result<ContactReadyPacket> = runCatching {
        val packetId = "contact-ready-$invitationId"
        val signature = signatureCrypto.sign(
            payloadEncoder.encodeReady(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                invitationId = invitationId,
                readyAtEpochMilliseconds = readyAtEpochMilliseconds,
                responseChallenge = responseChallenge,
                acceptedResponderEncryptionPublicKey = acceptedResponderEncryptionPublicKey,
                acceptedResponderSigningPublicKey = acceptedResponderSigningPublicKey,
                senderEncryptionPublicKey = senderEncryptionPublicKey,
                senderSigningPublicKey = signingKeyPair.publicKey
            ),
            signingKeyPair.privateKey
        ).getOrThrow()
        ContactReadyPacket(
            packetId = packetId,
            invitationId = invitationId,
            readyAtEpochMilliseconds = readyAtEpochMilliseconds,
            responseChallenge = responseChallenge.copyOf(),
            acceptedResponderEncryptionPublicKey = acceptedResponderEncryptionPublicKey.copyOf(),
            acceptedResponderSigningPublicKey = acceptedResponderSigningPublicKey.copyOf(),
            senderEncryptionPublicKey = senderEncryptionPublicKey.copyOf(),
            senderSigningPublicKey = signingKeyPair.publicKey.copyOf(),
            signature = signature.copyOf()
        )
    }

    private companion object {
        const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
    }
}
