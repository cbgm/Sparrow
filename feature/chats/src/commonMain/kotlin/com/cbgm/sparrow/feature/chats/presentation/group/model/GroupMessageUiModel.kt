package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel

data class GroupMessageUiModel(
    val bubble: MessageBubbleModel,
    val type: ChatMessageType,
    val senderContactId: String? = null,
    val senderProfilePictureBytes: ByteArray? = null
) {
    val id: String
        get() = bubble.id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupMessageUiModel

        if (bubble != other.bubble) return false
        if (type != other.type) return false
        if (senderContactId != other.senderContactId) return false
        if (!senderProfilePictureBytes.contentEquals(other.senderProfilePictureBytes)) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bubble.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (senderContactId?.hashCode() ?: 0)
        result = 31 * result + (senderProfilePictureBytes?.contentHashCode() ?: 0)
        result = 31 * result + id.hashCode()
        return result
    }
}
