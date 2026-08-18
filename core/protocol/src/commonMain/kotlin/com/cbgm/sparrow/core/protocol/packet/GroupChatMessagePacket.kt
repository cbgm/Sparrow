package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_chat_message")
data class GroupChatMessagePacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val epoch: Int,
    val messageId: String,
    val sentAtEpochMilliseconds: Long,
    val profilePicture: ProfilePictureMetadata = ProfilePictureMetadata(),
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val nonce: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ciphertext: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val senderSignature: ByteArray
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        require(sentAtEpochMilliseconds >= 0L) { "Message timestamp must not be negative" }
        require(nonce.isNotEmpty()) { "Group-message nonce must not be empty" }
        require(ciphertext.isNotEmpty()) { "Group-message ciphertext must not be empty" }
        require(senderSignature.isNotEmpty()) { "Group sender signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupChatMessagePacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            groupId == other.groupId &&
            epoch == other.epoch &&
            messageId == other.messageId &&
            sentAtEpochMilliseconds == other.sentAtEpochMilliseconds &&
            profilePicture == other.profilePicture &&
            nonce.contentEquals(other.nonce) &&
            ciphertext.contentEquals(other.ciphertext) &&
            senderSignature.contentEquals(other.senderSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + groupId.hashCode()
        result = 31 * result + epoch
        result = 31 * result + messageId.hashCode()
        result = 31 * result + sentAtEpochMilliseconds.hashCode()
        result = 31 * result + profilePicture.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + senderSignature.contentHashCode()
        return result
    }
}
