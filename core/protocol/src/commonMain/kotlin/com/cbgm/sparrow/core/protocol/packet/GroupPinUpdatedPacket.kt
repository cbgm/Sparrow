package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.message.GroupMessageContent
import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_pin_updated")
data class GroupPinUpdatedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val epoch: Int,
    val messageId: String? = null,
    val messageSentAtEpochMilliseconds: Long = 0L,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val messageSenderSigningPublicKey: ByteArray = byteArrayOf(),
    val messageContent: GroupMessageContent? = null,
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
        require(changedAtEpochMilliseconds > 0L) { "Group-pin timestamp must be positive" }
        require(adminSigningPublicKey.isNotEmpty()) { "Admin signing public key must not be empty" }
        require(adminSignature.isNotEmpty()) { "Admin signature must not be empty" }

        if (messageId == null) {
            require(messageSentAtEpochMilliseconds == 0L) { "Unpinned state must not contain a message timestamp" }
            require(messageSenderSigningPublicKey.isEmpty()) { "Unpinned state must not contain a message sender key" }
            require(messageContent == null) { "Unpinned state must not contain message content" }
        } else {
            require(messageId.isNotBlank()) { "Pinned message ID must not be blank" }
            require(messageSentAtEpochMilliseconds >= 0L) { "Pinned message timestamp must not be negative" }
            require(messageSenderSigningPublicKey.isNotEmpty()) { "Pinned message sender key must not be empty" }
            require(messageContent != null) { "Pinned state requires message content" }
        }
    }

    val hasPinnedMessage: Boolean
        get() = messageId != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupPinUpdatedPacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            messageId == other.messageId &&
            messageSentAtEpochMilliseconds == other.messageSentAtEpochMilliseconds &&
            messageSenderSigningPublicKey.contentEquals(other.messageSenderSigningPublicKey) &&
            messageContent == other.messageContent &&
            changedAtEpochMilliseconds == other.changedAtEpochMilliseconds &&
            adminSigningPublicKey.contentEquals(other.adminSigningPublicKey) &&
            adminSignature.contentEquals(other.adminSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + (messageId?.hashCode() ?: 0)
        result = 31 * result + messageSentAtEpochMilliseconds.hashCode()
        result = 31 * result + messageSenderSigningPublicKey.contentHashCode()
        result = 31 * result + (messageContent?.hashCode() ?: 0)
        result = 31 * result + changedAtEpochMilliseconds.hashCode()
        result = 31 * result + adminSigningPublicKey.contentHashCode()
        result = 31 * result + adminSignature.contentHashCode()
        return result
    }
}
