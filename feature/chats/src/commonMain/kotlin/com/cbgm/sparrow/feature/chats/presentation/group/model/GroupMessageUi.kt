package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType

data class GroupMessageUi(
    val type: ChatMessageType,
    val senderContactId: String? = null,
    val senderProfilePictureBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupMessageUi

        if (type != other.type) return false
        if (senderContactId != other.senderContactId) return false
        if (!senderProfilePictureBytes.contentEquals(other.senderProfilePictureBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (senderContactId?.hashCode() ?: 0)
        result = 31 * result + (senderProfilePictureBytes?.contentHashCode() ?: 0)
        return result
    }
}
