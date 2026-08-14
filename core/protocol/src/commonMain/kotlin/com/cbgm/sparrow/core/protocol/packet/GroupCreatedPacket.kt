package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_created")
data class GroupCreatedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val title: String,
    val createdAtEpochMilliseconds: Long,
    val epoch: Int,
    val members: List<GroupMemberPayload>,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val wrappedGroupKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerSignature: ByteArray,
    val membershipChange: GroupMembershipChangePayload? = null
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(title.isNotBlank()) { "Group title must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Group timestamp must not be negative" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(members.isNotEmpty()) { "A group packet requires at least one identity" }
        require(wrappedGroupKey.isNotEmpty()) { "Wrapped group key must not be empty" }
        require(ownerSignature.isNotEmpty()) { "Group owner signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupCreatedPacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            groupId == other.groupId &&
            title == other.title &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            epoch == other.epoch &&
            members == other.members &&
            wrappedGroupKey.contentEquals(other.wrappedGroupKey) &&
            ownerSignature.contentEquals(other.ownerSignature) &&
            membershipChange == other.membershipChange
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + groupId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        result = 31 * result + epoch
        result = 31 * result + members.hashCode()
        result = 31 * result + wrappedGroupKey.contentHashCode()
        result = 31 * result + ownerSignature.contentHashCode()
        result = 31 * result + (membershipChange?.hashCode() ?: 0)
        return result
    }
}

@Serializable
data class GroupMembershipChangePayload(
    val reason: String,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val memberSigningPublicKey: ByteArray
) {
    init {
        require(
            reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT ||
                reason == GroupMemberRemovedPacket.REASON_REMOVED_BY_OWNER
        ) {
            "Unsupported group membership change reason"
        }
        require(memberSigningPublicKey.isNotEmpty()) {
            "Group membership change requires a member signing key"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMembershipChangePayload) return false

        return reason == other.reason &&
            memberSigningPublicKey.contentEquals(other.memberSigningPublicKey)
    }

    override fun hashCode(): Int {
        var result = reason.hashCode()
        result = 31 * result + memberSigningPublicKey.contentHashCode()
        return result
    }
}
