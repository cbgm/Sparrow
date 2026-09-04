package com.cbgm.sparrow.feature.chats.presentation.overview.model

data class ConversationListItem(
    val conversationId: String,
    val contactId: String,
    val contactName: String,
    val avatarBytes: ByteArray? = null,
    val lastMessage: String = "",
    val hasMessages: Boolean = false,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConversationListItem

        if (hasMessages != other.hasMessages) return false
        if (unreadCount != other.unreadCount) return false
        if (isGroup != other.isGroup) return false
        if (conversationId != other.conversationId) return false
        if (contactId != other.contactId) return false
        if (contactName != other.contactName) return false
        if (!avatarBytes.contentEquals(other.avatarBytes)) return false
        if (lastMessage != other.lastMessage) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hasMessages.hashCode()
        result = 31 * result + unreadCount
        result = 31 * result + isGroup.hashCode()
        result = 31 * result + conversationId.hashCode()
        result = 31 * result + contactId.hashCode()
        result = 31 * result + contactName.hashCode()
        result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
        result = 31 * result + lastMessage.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
