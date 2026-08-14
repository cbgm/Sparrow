package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_conversation_deleted")
data class GroupConversationDeletedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val invitationId: String,
    val groupId: String,
    val epoch: Int,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val challenge: ByteArray,
    val deletedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch >= PENDING_GROUP_EPOCH) { "Group deletion epoch must not be negative" }
        require(challenge.isNotEmpty()) { "Invitation challenge must not be empty" }
        require(deletedAtEpochMilliseconds >= 0L) { "Group deletion timestamp must not be negative" }
        require(ownerSignature.isNotEmpty()) { "Group owner signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupConversationDeletedPacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            invitationId == other.invitationId &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            challenge.contentEquals(other.challenge) &&
            deletedAtEpochMilliseconds == other.deletedAtEpochMilliseconds &&
            ownerSignature.contentEquals(other.ownerSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + deletedAtEpochMilliseconds.hashCode()
        result = 31 * result + ownerSignature.contentHashCode()
        return result
    }

    companion object {
        const val PENDING_GROUP_EPOCH = 0
    }
}
