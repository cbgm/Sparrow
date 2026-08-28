package com.cbgm.sparrow.notification.domain.model

sealed interface NotificationConversationTarget {
    data class Direct(
        val conversationId: String,
        val contactId: String,
        val contactName: String
    ) : NotificationConversationTarget

    data class Group(
        val conversationId: String
    ) : NotificationConversationTarget
}
