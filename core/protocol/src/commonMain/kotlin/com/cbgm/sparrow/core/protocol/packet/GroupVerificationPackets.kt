package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupVerificationMemberPayload(
    val invitationId: String,
    val displayName: String,
    val membershipStatus: String,
    val adminVerifiedParticipant: Boolean,
    val participantVerifiedAdmin: Boolean
) {
    init {
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(displayName.isNotBlank()) { "Group member display name must not be blank" }
        require(membershipStatus == ACTIVE_STATUS || membershipStatus == PENDING_STATUS) {
            "Unsupported group verification membership status"
        }
        require(
            membershipStatus == ACTIVE_STATUS ||
                (!adminVerifiedParticipant && !participantVerifiedAdmin)
        ) {
            "Pending group members must not be marked as verified"
        }
    }

    val isMutuallyVerified: Boolean
        get() = adminVerifiedParticipant && participantVerifiedAdmin

    companion object {
        const val ACTIVE_STATUS = "ACTIVE"
        const val PENDING_STATUS = "PENDING"
    }
}

@Serializable
@SerialName("group_verification_receipt")
data class GroupVerificationReceiptPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val invitationId: String,
    val receiptId: String,
    val verifiedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val participantEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val participantSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(receiptId.isNotBlank()) { "Receipt ID must not be blank" }
        require(verifiedAtEpochMilliseconds >= 0L) { "Verification timestamp must not be negative" }
        require(participantEncryptionPublicKey.size == PUBLIC_KEY_SIZE) {
            "Participant encryption key must contain 32 bytes"
        }
        require(participantSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Participant signing key must contain 32 bytes" }
        require(ownerEncryptionPublicKey.size == PUBLIC_KEY_SIZE) { "Owner encryption key must contain 32 bytes" }
        require(ownerSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Owner signing key must contain 32 bytes" }
        require(signature.size == SIGNATURE_SIZE) { "Verification signature must contain 64 bytes" }
    }
}

@Serializable
@SerialName("group_verification_snapshot_request")
data class GroupVerificationSnapshotRequestPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val invitationId: String,
    val requestId: String,
    val requestedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val requesterSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(requestId.isNotBlank()) { "Request ID must not be blank" }
        require(requestedAtEpochMilliseconds >= 0L) { "Request timestamp must not be negative" }
        require(requesterSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Requester signing key must contain 32 bytes" }
        require(signature.size == SIGNATURE_SIZE) { "Request signature must contain 64 bytes" }
    }
}

@Serializable
@SerialName("group_verification_snapshot")
data class GroupVerificationSnapshotPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val snapshotId: String,
    val generatedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerSigningPublicKey: ByteArray,
    val members: List<GroupVerificationMemberPayload>,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(snapshotId.isNotBlank()) { "Snapshot ID must not be blank" }
        require(generatedAtEpochMilliseconds >= 0L) { "Snapshot timestamp must not be negative" }
        require(ownerEncryptionPublicKey.size == PUBLIC_KEY_SIZE) { "Owner encryption key must contain 32 bytes" }
        require(ownerSigningPublicKey.size == PUBLIC_KEY_SIZE) { "Owner signing key must contain 32 bytes" }
        require(members.distinctBy(GroupVerificationMemberPayload::invitationId).size == members.size) {
            "Group verification snapshot contains duplicate invitations"
        }
        require(signature.size == SIGNATURE_SIZE) { "Snapshot signature must contain 64 bytes" }
    }
}

private const val PUBLIC_KEY_SIZE = 32
private const val SIGNATURE_SIZE = 64
