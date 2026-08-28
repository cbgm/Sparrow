package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi

data class GroupMessageUi(
    val bubble: MessageBubbleUi,
    val type: ChatMessageType,
    val senderContactId: String? = null,
    val senderProfilePictureBytes: ByteArray? = null
) {
    val id: String
        get() = bubble.id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMessageUi) return false

        return bubble == other.bubble &&
            type == other.type &&
            senderContactId == other.senderContactId &&
            senderProfilePictureBytes.contentEquals(other.senderProfilePictureBytes)
    }

    override fun hashCode(): Int {
        var result = bubble.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (senderContactId?.hashCode() ?: 0)
        result = 31 * result + (senderProfilePictureBytes?.contentHashCode() ?: 0)
        return result
    }
}
