package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_invite")
data class GroupInvitePacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val invitationId: String,
    val groupId: String,
    val title: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val challenge: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerEncryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(title.isNotBlank()) { "Group title must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Invitation timestamp must not be negative" }
        require(expiresAtEpochMilliseconds > createdAtEpochMilliseconds) {
            "Invitation expiration must be after its creation"
        }
        require(challenge.isNotEmpty()) { "Invitation challenge must not be empty" }
        require(ownerEncryptionPublicKey.isNotEmpty()) { "Owner encryption public key must not be empty" }
        require(ownerSigningPublicKey.isNotEmpty()) { "Owner signing public key must not be empty" }
        require(ownerSignature.isNotEmpty()) { "Owner signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupInvitePacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            invitationId == other.invitationId &&
            groupId == other.groupId &&
            title == other.title &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            expiresAtEpochMilliseconds == other.expiresAtEpochMilliseconds &&
            challenge.contentEquals(other.challenge) &&
            ownerEncryptionPublicKey.contentEquals(other.ownerEncryptionPublicKey) &&
            ownerSigningPublicKey.contentEquals(other.ownerSigningPublicKey) &&
            ownerSignature.contentEquals(other.ownerSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + ownerEncryptionPublicKey.contentHashCode()
        result = 31 * result + ownerSigningPublicKey.contentHashCode()
        result = 31 * result + ownerSignature.contentHashCode()
        return result
    }
}
