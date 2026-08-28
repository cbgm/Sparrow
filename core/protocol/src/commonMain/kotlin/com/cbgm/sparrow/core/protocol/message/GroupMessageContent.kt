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
    val attachments: List<MessageAttachment> = emptyList()
) {
    init {
        require(text.isNotBlank() || attachments.isNotEmpty()) {
            "Group message must contain text or attachments"
        }
        require(attachments.size <= MessageAttachmentConstraints.MAX_ATTACHMENTS_PER_MESSAGE) {
            "Group message can contain at most ${MessageAttachmentConstraints.MAX_ATTACHMENTS_PER_MESSAGE} attachments"
        }
        require(attachments.map(MessageAttachment::attachmentId).distinct().size == attachments.size) {
            "Group attachment IDs must be unique"
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
