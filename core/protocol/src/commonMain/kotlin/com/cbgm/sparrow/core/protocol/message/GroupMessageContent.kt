package com.cbgm.sparrow.core.protocol.message

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentConstraints
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GroupMessageContent(
    val text: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val attachments: List<MessageAttachment> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val replyToMessageId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val reaction: MessageReactionPayload? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val deletion: MessageDeletionPayload? = null
) {
    init {
        require(text.isNotBlank() || attachments.isNotEmpty() || reaction != null || deletion != null) {
            "Group message must contain text, attachments, a reaction, or a deletion"
        }
        require(reaction == null || deletion == null) {
            "Group message must not contain both a reaction and a deletion"
        }
        require(
            (reaction == null && deletion == null) ||
                (text.isBlank() && attachments.isEmpty() && replyToMessageId == null)
        ) {
            "Group control content must not contain message content or a reply target"
        }
        require(attachments.size <= MessageAttachmentConstraints.MAX_ATTACHMENTS_PER_MESSAGE) {
            "Group message can contain at most ${MessageAttachmentConstraints.MAX_ATTACHMENTS_PER_MESSAGE} attachments"
        }
        require(attachments.map(MessageAttachment::attachmentId).distinct().size == attachments.size) {
            "Group attachment IDs must be unique"
        }
        require(replyToMessageId == null || replyToMessageId.isNotBlank()) {
            "Reply message ID must not be blank"
        }
    }
}

class GroupMessageContentCodec(
    private val json: Json
) {
    fun encode(content: GroupMessageContent): String =
        FORMAT_PREFIX + json.encodeToString(content)

    fun decode(plaintext: String): GroupMessageContent =
        if (plaintext.startsWith(FORMAT_PREFIX)) {
            json.decodeFromString<GroupMessageContent>(plaintext.removePrefix(FORMAT_PREFIX))
        } else {
            GroupMessageContent(text = plaintext)
        }

    private companion object {
        const val FORMAT_PREFIX = "sparrow-group-message-v2:"
    }
}
