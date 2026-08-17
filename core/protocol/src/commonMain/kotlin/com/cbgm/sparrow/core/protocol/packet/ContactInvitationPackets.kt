package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("contact_invite")
data class ContactInvitePacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val invitationId: String,
    val displayName: String?,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    val profilePicture: ProfilePictureMetadata = ProfilePictureMetadata(),
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val inviteChallenge: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val encryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signingPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Invitation timestamp must not be negative" }
        require(expiresAtEpochMilliseconds > createdAtEpochMilliseconds) { "Invitation expiry must be after creation" }
        require(inviteChallenge.size == CHALLENGE_SIZE) { "Invitation challenge must contain $CHALLENGE_SIZE bytes" }
        require(encryptionPublicKey.size == PUBLIC_KEY_SIZE) { "Encryption public key must contain $PUBLIC_KEY_SIZE bytes" }
        require(signingPublicKey.size == PUBLIC_KEY_SIZE) { "Signing public key must contain $PUBLIC_KEY_SIZE bytes" }
        require(signature.size == SIGNATURE_SIZE) { "Invitation signature must contain $SIGNATURE_SIZE bytes" }
    }

    override fun equals(other: Any?): Boolean =
        other is ContactInvitePacket &&
            packetId == other.packetId &&
            version == other.version &&
            invitationId == other.invitationId &&
            displayName == other.displayName &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            expiresAtEpochMilliseconds == other.expiresAtEpochMilliseconds &&
            profilePicture == other.profilePicture &&
            inviteChallenge.contentEquals(other.inviteChallenge) &&
            encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
            signingPublicKey.contentEquals(other.signingPublicKey) &&
            signature.contentEquals(other.signature)

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + profilePicture.hashCode()
        result = 31 * result + inviteChallenge.contentHashCode()
        result = 31 * result + encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

@Serializable
@SerialName("contact_invite_accepted")
data class ContactInviteAcceptedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val invitationId: String,
    val acceptedAtEpochMilliseconds: Long,
    val profilePicture: ProfilePictureMetadata = ProfilePictureMetadata(),
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val inviteChallenge: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val responseChallenge: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val inviterEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val inviterSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val responderEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val responderSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(acceptedAtEpochMilliseconds >= 0L) { "Acceptance timestamp must not be negative" }
        require(inviteChallenge.size == CHALLENGE_SIZE) { "Invitation challenge must contain $CHALLENGE_SIZE bytes" }
        require(responseChallenge.size == CHALLENGE_SIZE) { "Response challenge must contain $CHALLENGE_SIZE bytes" }
        require(inviterEncryptionPublicKey.size == PUBLIC_KEY_SIZE) { "Inviter encryption key must contain $PUBLIC_KEY_SIZE bytes" }
        require(inviterSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Inviter signing key must contain $PUBLIC_KEY_SIZE bytes" }
        require(responderEncryptionPublicKey.size == PUBLIC_KEY_SIZE) { "Responder encryption key must contain $PUBLIC_KEY_SIZE bytes" }
        require(responderSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Responder signing key must contain $PUBLIC_KEY_SIZE bytes" }
        require(signature.size == SIGNATURE_SIZE) { "Acceptance signature must contain $SIGNATURE_SIZE bytes" }
    }

    override fun equals(other: Any?): Boolean =
        other is ContactInviteAcceptedPacket &&
            packetId == other.packetId &&
            version == other.version &&
            invitationId == other.invitationId &&
            acceptedAtEpochMilliseconds == other.acceptedAtEpochMilliseconds &&
            profilePicture == other.profilePicture &&
            inviteChallenge.contentEquals(other.inviteChallenge) &&
            responseChallenge.contentEquals(other.responseChallenge) &&
            inviterEncryptionPublicKey.contentEquals(other.inviterEncryptionPublicKey) &&
            inviterSigningPublicKey.contentEquals(other.inviterSigningPublicKey) &&
            responderEncryptionPublicKey.contentEquals(other.responderEncryptionPublicKey) &&
            responderSigningPublicKey.contentEquals(other.responderSigningPublicKey) &&
            signature.contentEquals(other.signature)

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + acceptedAtEpochMilliseconds.hashCode()
        result = 31 * result + profilePicture.hashCode()
        result = 31 * result + inviteChallenge.contentHashCode()
        result = 31 * result + responseChallenge.contentHashCode()
        result = 31 * result + inviterEncryptionPublicKey.contentHashCode()
        result = 31 * result + inviterSigningPublicKey.contentHashCode()
        result = 31 * result + responderEncryptionPublicKey.contentHashCode()
        result = 31 * result + responderSigningPublicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

@Serializable
@SerialName("contact_ready")
data class ContactReadyPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val invitationId: String,
    val readyAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val responseChallenge: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val acceptedResponderEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val acceptedResponderSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val senderEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val senderSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(readyAtEpochMilliseconds >= 0L) { "Ready timestamp must not be negative" }
        require(responseChallenge.size == CHALLENGE_SIZE) { "Response challenge must contain $CHALLENGE_SIZE bytes" }
        require(acceptedResponderEncryptionPublicKey.size == PUBLIC_KEY_SIZE) {
            "Accepted responder encryption key must contain $PUBLIC_KEY_SIZE bytes"
        }
        require(acceptedResponderSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Accepted responder signing key must contain $PUBLIC_KEY_SIZE bytes" }
        require(senderEncryptionPublicKey.size == PUBLIC_KEY_SIZE) { "Sender encryption key must contain $PUBLIC_KEY_SIZE bytes" }
        require(senderSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Sender signing key must contain $PUBLIC_KEY_SIZE bytes" }
        require(signature.size == SIGNATURE_SIZE) { "Ready signature must contain $SIGNATURE_SIZE bytes" }
    }

    override fun equals(other: Any?): Boolean =
        other is ContactReadyPacket &&
            packetId == other.packetId &&
            version == other.version &&
            invitationId == other.invitationId &&
            readyAtEpochMilliseconds == other.readyAtEpochMilliseconds &&
            responseChallenge.contentEquals(other.responseChallenge) &&
            acceptedResponderEncryptionPublicKey.contentEquals(other.acceptedResponderEncryptionPublicKey) &&
            acceptedResponderSigningPublicKey.contentEquals(other.acceptedResponderSigningPublicKey) &&
            senderEncryptionPublicKey.contentEquals(other.senderEncryptionPublicKey) &&
            senderSigningPublicKey.contentEquals(other.senderSigningPublicKey) &&
            signature.contentEquals(other.signature)

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + readyAtEpochMilliseconds.hashCode()
        result = 31 * result + responseChallenge.contentHashCode()
        result = 31 * result + acceptedResponderEncryptionPublicKey.contentHashCode()
        result = 31 * result + acceptedResponderSigningPublicKey.contentHashCode()
        result = 31 * result + senderEncryptionPublicKey.contentHashCode()
        result = 31 * result + senderSigningPublicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

@Serializable
@SerialName("contact_verification_receipt")
data class ContactVerificationReceiptPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val receiptId: String,
    val verifiedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val senderEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val senderSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val verifiedEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val verifiedSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(receiptId.isNotBlank()) { "Receipt ID must not be blank" }
        require(verifiedAtEpochMilliseconds >= 0L) { "Verification timestamp must not be negative" }
        require(senderEncryptionPublicKey.size == PUBLIC_KEY_SIZE) { "Sender encryption key must contain $PUBLIC_KEY_SIZE bytes" }
        require(senderSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Sender signing key must contain $PUBLIC_KEY_SIZE bytes" }
        require(verifiedEncryptionPublicKey.size == PUBLIC_KEY_SIZE) { "Verified encryption key must contain $PUBLIC_KEY_SIZE bytes" }
        require(verifiedSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Verified signing key must contain $PUBLIC_KEY_SIZE bytes" }
        require(signature.size == SIGNATURE_SIZE) { "Verification signature must contain $SIGNATURE_SIZE bytes" }
    }

    override fun equals(other: Any?): Boolean =
        other is ContactVerificationReceiptPacket &&
            packetId == other.packetId &&
            version == other.version &&
            receiptId == other.receiptId &&
            verifiedAtEpochMilliseconds == other.verifiedAtEpochMilliseconds &&
            senderEncryptionPublicKey.contentEquals(other.senderEncryptionPublicKey) &&
            senderSigningPublicKey.contentEquals(other.senderSigningPublicKey) &&
            verifiedEncryptionPublicKey.contentEquals(other.verifiedEncryptionPublicKey) &&
            verifiedSigningPublicKey.contentEquals(other.verifiedSigningPublicKey) &&
            signature.contentEquals(other.signature)

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + receiptId.hashCode()
        result = 31 * result + verifiedAtEpochMilliseconds.hashCode()
        result = 31 * result + senderEncryptionPublicKey.contentHashCode()
        result = 31 * result + senderSigningPublicKey.contentHashCode()
        result = 31 * result + verifiedEncryptionPublicKey.contentHashCode()
        result = 31 * result + verifiedSigningPublicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

@Serializable
@SerialName("contact_invite_declined")
data class ContactInviteDeclinedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val invitationId: String,
    val declinedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val inviteChallenge: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val declinerSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(declinedAtEpochMilliseconds >= 0L) { "Decline timestamp must not be negative" }
        require(inviteChallenge.size == CHALLENGE_SIZE) { "Invitation challenge must contain $CHALLENGE_SIZE bytes" }
        require(declinerSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Decliner signing key must contain $PUBLIC_KEY_SIZE bytes" }
        require(signature.size == SIGNATURE_SIZE) { "Decline signature must contain $SIGNATURE_SIZE bytes" }
    }

    override fun equals(other: Any?): Boolean =
        other is ContactInviteDeclinedPacket &&
            packetId == other.packetId &&
            version == other.version &&
            invitationId == other.invitationId &&
            declinedAtEpochMilliseconds == other.declinedAtEpochMilliseconds &&
            inviteChallenge.contentEquals(other.inviteChallenge) &&
            declinerSigningPublicKey.contentEquals(other.declinerSigningPublicKey) &&
            signature.contentEquals(other.signature)

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + declinedAtEpochMilliseconds.hashCode()
        result = 31 * result + inviteChallenge.contentHashCode()
        result = 31 * result + declinerSigningPublicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

@Serializable
@SerialName("direct_chat_authorization_revoked")
data class DirectChatAuthorizationRevokedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val invitationId: String,
    val revokedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val inviteChallenge: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val revokerSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(revokedAtEpochMilliseconds >= 0L) { "Revocation timestamp must not be negative" }
        require(inviteChallenge.size == CHALLENGE_SIZE) { "Invitation challenge must contain $CHALLENGE_SIZE bytes" }
        require(revokerSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Revoker signing key must contain $PUBLIC_KEY_SIZE bytes" }
        require(signature.size == SIGNATURE_SIZE) { "Revocation signature must contain $SIGNATURE_SIZE bytes" }
    }

    override fun equals(other: Any?): Boolean =
        other is DirectChatAuthorizationRevokedPacket &&
            packetId == other.packetId &&
            version == other.version &&
            invitationId == other.invitationId &&
            revokedAtEpochMilliseconds == other.revokedAtEpochMilliseconds &&
            inviteChallenge.contentEquals(other.inviteChallenge) &&
            revokerSigningPublicKey.contentEquals(other.revokerSigningPublicKey) &&
            signature.contentEquals(other.signature)

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + revokedAtEpochMilliseconds.hashCode()
        result = 31 * result + inviteChallenge.contentHashCode()
        result = 31 * result + revokerSigningPublicKey.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

private const val CHALLENGE_SIZE = 32
private const val PUBLIC_KEY_SIZE = 32
private const val SIGNATURE_SIZE = 64
