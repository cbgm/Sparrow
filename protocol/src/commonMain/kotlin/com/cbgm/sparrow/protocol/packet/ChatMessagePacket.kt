package com.cbgm.sparrow.protocol.packet

import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.protocol.message.MessageReactionPayload
import com.cbgm.sparrow.protocol.messagepart.requireValidWireMessageParts
import com.cbgm.sparrow.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.protocol.version.ProtocolVersion
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("chat_message")
data class ChatMessagePacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    /**
     * Stable ID of the logical chat message.
     *
     * It may initially be the same as packetId, but keeping it
     * separate allows a message to be retransmitted in another packet.
     */
    val messageId: String,
    val sentAtEpochMilliseconds: Long,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val parts: List<MessagePartDto> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val replyToMessageId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val reaction: MessageReactionPayload? = null,
    val senderPhoneNumber: String? = null,
    val profilePicture: ProfilePictureMetadata = ProfilePictureMetadata()
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        require(sentAtEpochMilliseconds >= 0L) { "Message timestamp must not be negative" }

        if (reaction == null) {
            parts.requireValidWireMessageParts(expectedTextPartId = messageId)
        } else {
            require(parts.isEmpty() && replyToMessageId == null) {
                "Reaction packets must not contain message content or a reply target"
            }
        }

        require(replyToMessageId == null || replyToMessageId.isNotBlank()) {
            "Reply message ID must not be blank"
        }
        require(senderPhoneNumber == null || senderPhoneNumber.isNotBlank()) {
            "Sender phone number must not be blank"
        }
    }
}
