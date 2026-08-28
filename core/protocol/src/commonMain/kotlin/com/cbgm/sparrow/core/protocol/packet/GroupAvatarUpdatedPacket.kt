package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarMetadata
import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_avatar_updated")
data class GroupAvatarUpdatedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val epoch: Int,
    val avatar: GroupAvatarMetadata,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val adminSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val adminSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(adminSigningPublicKey.isNotEmpty()) { "Admin signing public key must not be empty" }
        require(adminSignature.isNotEmpty()) { "Admin signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupAvatarUpdatedPacket) return false
        return packetId == other.packetId &&
            version == other.version &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            avatar == other.avatar &&
            adminSigningPublicKey.contentEquals(other.adminSigningPublicKey) &&
            adminSignature.contentEquals(other.adminSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + avatar.hashCode()
        result = 31 * result + adminSigningPublicKey.contentHashCode()
        result = 31 * result + adminSignature.contentHashCode()
        return result
    }
}
