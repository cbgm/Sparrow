package com.cbgm.sparrow.protocol.message

import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.protocol.messagepart.requireValidWireMessageParts
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GroupMessageContent(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val parts: List<MessagePartDto> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val replyToMessageId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val reaction: MessageReactionPayload? = null
) {
    init {
        if (reaction == null) {
            parts.requireValidWireMessageParts()
        } else {
            require(parts.isEmpty() && replyToMessageId == null) {
                "Group reaction content must not contain message content or a reply target"
            }
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
            GroupMessageContent(
                parts = listOf(
                    TextDto(
                        id = LEGACY_TEXT_PART_ID,
                        text = plaintext
                    )
                )
            )
        }

    private companion object {
        const val FORMAT_PREFIX = "sparrow-group-message-v2:"
        const val LEGACY_TEXT_PART_ID = "legacy-text"
    }
}
