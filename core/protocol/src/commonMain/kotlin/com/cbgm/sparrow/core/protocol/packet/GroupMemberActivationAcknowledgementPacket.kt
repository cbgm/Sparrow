package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_member_activation_acknowledgement")
data class GroupMemberActivationAcknowledgementPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val epoch: Int,
    val activationPacketId: String,
    val activationId: String,
    val activationRound: Int,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val activatedMemberSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val acknowledgingMemberSigningPublicKey: ByteArray,
    val acknowledgedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val memberSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(activationPacketId.isNotBlank()) { "Activation packet ID must not be blank" }
        require(activationId.isNotBlank()) { "Activation ID must not be blank" }
        require(activationRound in GroupMemberActivatedPacket.DISCOVERY_ROUND..GroupMemberActivatedPacket.RECIPROCAL_ROUND) {
            "Acknowledged activation round must be 1 or 2"
        }
        require(activatedMemberSigningPublicKey.isNotEmpty()) { "Activated member signing key must not be empty" }
        require(acknowledgingMemberSigningPublicKey.isNotEmpty()) { "Acknowledging member signing key must not be empty" }
        require(acknowledgedAtEpochMilliseconds >= 0L) { "Acknowledgement timestamp must not be negative" }
        require(memberSignature.isNotEmpty()) { "Member signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMemberActivationAcknowledgementPacket) return false
        return packetId == other.packetId &&
            version == other.version &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            activationPacketId == other.activationPacketId &&
            activationId == other.activationId &&
            activationRound == other.activationRound &&
            activatedMemberSigningPublicKey.contentEquals(other.activatedMemberSigningPublicKey) &&
            acknowledgingMemberSigningPublicKey.contentEquals(other.acknowledgingMemberSigningPublicKey) &&
            acknowledgedAtEpochMilliseconds == other.acknowledgedAtEpochMilliseconds &&
            memberSignature.contentEquals(other.memberSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + activationPacketId.hashCode()
        result = 31 * result + activationId.hashCode()
        result = 31 * result + activationRound
        result = 31 * result + activatedMemberSigningPublicKey.contentHashCode()
        result = 31 * result + acknowledgingMemberSigningPublicKey.contentHashCode()
        result = 31 * result + acknowledgedAtEpochMilliseconds.hashCode()
        result = 31 * result + memberSignature.contentHashCode()
        return result
    }
}
