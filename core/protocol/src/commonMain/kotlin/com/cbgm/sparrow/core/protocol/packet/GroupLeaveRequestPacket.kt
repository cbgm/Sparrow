package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_leave_request")
data class GroupLeaveRequestPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val invitationId: String,
    val groupId: String,
    val epoch: Int,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val challenge: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val memberSigningPublicKey: ByteArray,
    val requestedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val memberSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(challenge.isNotEmpty()) { "Invitation challenge must not be empty" }
        require(memberSigningPublicKey.isNotEmpty()) { "Member signing public key must not be empty" }
        require(requestedAtEpochMilliseconds >= 0L) { "Leave request timestamp must not be negative" }
        require(memberSignature.isNotEmpty()) { "Member signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupLeaveRequestPacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            invitationId == other.invitationId &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            challenge.contentEquals(other.challenge) &&
            memberSigningPublicKey.contentEquals(other.memberSigningPublicKey) &&
            requestedAtEpochMilliseconds == other.requestedAtEpochMilliseconds &&
            memberSignature.contentEquals(other.memberSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + invitationId.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + memberSigningPublicKey.contentHashCode()
        result = 31 * result + requestedAtEpochMilliseconds.hashCode()
        result = 31 * result + memberSignature.contentHashCode()
        return result
    }
}
