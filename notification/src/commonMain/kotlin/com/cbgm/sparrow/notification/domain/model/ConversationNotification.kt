package com.cbgm.sparrow.notification.domain.model

data class ConversationNotification(
    val conversationId: String,
    val title: String,
    val messagePreview: String?,
    val unreadCount: Int
)
