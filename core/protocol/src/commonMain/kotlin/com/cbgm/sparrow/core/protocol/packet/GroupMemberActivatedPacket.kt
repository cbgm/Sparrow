package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_member_activated")
data class GroupMemberActivatedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val epoch: Int,
    val activationId: String,
    val member: GroupMemberPayload,
    val activatedAtEpochMilliseconds: Long,
    val activationRound: Int,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(activationId.isNotBlank()) { "Activation ID must not be blank" }
        require(member.role in GROUP_MEMBER_ROLES) { "Activated group member uses an unsupported role" }
        require(member.encryptionPublicKey.isNotEmpty()) { "Activated group member requires an encryption key" }
        require(member.signingPublicKey.isNotEmpty()) { "Activated group member requires a signing key" }
        require(activatedAtEpochMilliseconds >= 0L) { "Activation timestamp must not be negative" }
        require(activationRound in FINAL_ROUND..RECIPROCAL_ROUND) { "Activation round must be between 0 and 2" }
        require(ownerSignature.isNotEmpty()) { "Owner signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMemberActivatedPacket) return false
        return packetId == other.packetId &&
            version == other.version &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            activationId == other.activationId &&
            member == other.member &&
            activatedAtEpochMilliseconds == other.activatedAtEpochMilliseconds &&
            activationRound == other.activationRound &&
            ownerSignature.contentEquals(other.ownerSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + activationId.hashCode()
        result = 31 * result + member.hashCode()
        result = 31 * result + activatedAtEpochMilliseconds.hashCode()
        result = 31 * result + activationRound
        result = 31 * result + ownerSignature.contentHashCode()
        return result
    }

    companion object {
        const val FINAL_ROUND = 0
        const val DISCOVERY_ROUND = 1
        const val RECIPROCAL_ROUND = 2
        private val GROUP_MEMBER_ROLES = setOf("OWNER", "ADMIN", "MEMBER")
    }
}
