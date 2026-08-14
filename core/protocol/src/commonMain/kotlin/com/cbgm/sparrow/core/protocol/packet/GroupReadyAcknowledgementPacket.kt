package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_ready_acknowledgement")
data class GroupReadyAcknowledgementPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val epoch: Int,
    val welcomePacketId: String,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val keyConfirmation: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val memberSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(welcomePacketId.isNotBlank()) { "Welcome packet ID must not be blank" }
        require(keyConfirmation.isNotEmpty()) { "Group key confirmation must not be empty" }
        require(memberSignature.isNotEmpty()) { "Member signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupReadyAcknowledgementPacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            welcomePacketId == other.welcomePacketId &&
            keyConfirmation.contentEquals(other.keyConfirmation) &&
            memberSignature.contentEquals(other.memberSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + welcomePacketId.hashCode()
        result = 31 * result + keyConfirmation.contentHashCode()
        result = 31 * result + memberSignature.contentHashCode()
        return result
    }
}
