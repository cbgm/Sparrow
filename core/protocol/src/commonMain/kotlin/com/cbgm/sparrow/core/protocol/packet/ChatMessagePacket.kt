package com.cbgm.sparrow.core.protocol.packet

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentConstraints
import com.cbgm.sparrow.core.protocol.message.MessageDeletionPayload
import com.cbgm.sparrow.core.protocol.message.MessageReactionPayload
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
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
    val text: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val attachments: List<MessageAttachment> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val replyToMessageId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val reaction: MessageReactionPayload? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val deletion: MessageDeletionPayload? = null,
    val senderPhoneNumber: String? = null,
    val profilePicture: ProfilePictureMetadata = ProfilePictureMetadata()
) : SparrowPacket {
    init {
        require(packetId.isNotBlank()) {
            "Packet ID must not be blank"
        }

        require(version > 0) {
            "Protocol version must be positive"
        }

        require(messageId.isNotBlank()) {
            "Message ID must not be blank"
        }

        require(sentAtEpochMilliseconds >= 0L) {
            "Message timestamp must not be negative"
        }

        require(text.isNotBlank() || attachments.isNotEmpty() || reaction != null || deletion != null) {
            "Message must contain text, an attachment, a reaction, or a deletion"
        }

        require(reaction == null || deletion == null) {
            "Message packet must not contain both a reaction and a deletion"
        }

        require(
            (reaction == null && deletion == null) ||
                (text.isBlank() && attachments.isEmpty() && replyToMessageId == null)
        ) {
            "Control packets must not contain message content or a reply target"
        }

        require(attachments.size <= MessageAttachmentConstraints.MAX_ATTACHMENTS_PER_MESSAGE) {
            "A message can contain at most ${MessageAttachmentConstraints.MAX_ATTACHMENTS_PER_MESSAGE} attachments"
        }

        require(attachments.map(MessageAttachment::attachmentId).distinct().size == attachments.size) {
            "Attachment IDs must be unique within a message"
        }

        require(replyToMessageId == null || replyToMessageId.isNotBlank()) {
            "Reply message ID must not be blank"
        }

        require(senderPhoneNumber == null || senderPhoneNumber.isNotBlank()) {
            "Sender phone number must not be blank"
        }
    }
}
