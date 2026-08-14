package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_member_removed")
data class GroupMemberRemovedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val invitationId: String,
    val groupId: String,
    val epoch: Int,
    val reason: String = REASON_REMOVED_BY_OWNER,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val challenge: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val removedMemberSigningPublicKey: ByteArray,
    val removedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch == PENDING_INVITATION_EPOCH || epoch > 1) {
            "Removal must cancel a pending invitation or advance the group epoch"
        }
        require(reason == REASON_REMOVED_BY_OWNER || reason == REASON_MEMBER_LEFT) {
            "Unsupported group membership removal reason"
        }
        if (reason == REASON_MEMBER_LEFT) {
            require(epoch > PENDING_INVITATION_EPOCH) {
                "A voluntary leave must advance the group epoch"
            }
        }
        if (epoch == PENDING_INVITATION_EPOCH) {
            require(challenge.isNotEmpty()) { "Pending invitation challenge must not be empty" }
        }
        if (epoch > PENDING_INVITATION_EPOCH) {
            require(removedMemberSigningPublicKey.isNotEmpty()) {
                "Active removed member signing key must not be empty"
            }
        }
        require(removedAtEpochMilliseconds >= 0L) { "Removal timestamp must not be negative" }
        require(ownerSignature.isNotEmpty()) { "Group owner signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMemberRemovedPacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            invitationId == other.invitationId &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            reason == other.reason &&
            challenge.contentEquals(other.challenge) &&
            removedMemberSigningPublicKey.contentEquals(other.removedMemberSigningPublicKey) &&
            removedAtEpochMilliseconds == other.removedAtEpochMilliseconds &&
            ownerSignature.contentEquals(other.ownerSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + reason.hashCode()
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + removedMemberSigningPublicKey.contentHashCode()
        result = 31 * result + removedAtEpochMilliseconds.hashCode()
        result = 31 * result + ownerSignature.contentHashCode()
        return result
    }

    companion object {
        const val PENDING_INVITATION_EPOCH = 0
        const val REASON_REMOVED_BY_OWNER = "REMOVED_BY_OWNER"
        const val REASON_MEMBER_LEFT = "MEMBER_LEFT"
    }
}
