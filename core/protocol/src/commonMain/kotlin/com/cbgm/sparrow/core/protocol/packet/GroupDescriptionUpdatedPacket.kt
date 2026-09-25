package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_description_updated")
data class GroupDescriptionUpdatedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val epoch: Int,
    val description: String?,
    val changedAtEpochMilliseconds: Long,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val adminSigningPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val adminSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(description == null || description.isNotBlank()) {
            "Group description must be null or non-blank"
        }
        require(description == null || description.length <= MAX_GROUP_DESCRIPTION_LENGTH) {
            "Group description must not exceed $MAX_GROUP_DESCRIPTION_LENGTH characters"
        }
        require(changedAtEpochMilliseconds > 0L) { "Group-description timestamp must be positive" }
        require(adminSigningPublicKey.isNotEmpty()) { "Admin signing public key must not be empty" }
        require(adminSignature.isNotEmpty()) { "Admin signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupDescriptionUpdatedPacket) return false
        return packetId == other.packetId &&
            version == other.version &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            description == other.description &&
            changedAtEpochMilliseconds == other.changedAtEpochMilliseconds &&
            adminSigningPublicKey.contentEquals(other.adminSigningPublicKey) &&
            adminSignature.contentEquals(other.adminSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + changedAtEpochMilliseconds.hashCode()
        result = 31 * result + adminSigningPublicKey.contentHashCode()
        result = 31 * result + adminSignature.contentHashCode()
        return result
    }

    private companion object {
        const val MAX_GROUP_DESCRIPTION_LENGTH = 500
    }
}
