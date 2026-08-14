package com.cbgm.securechat.feature.chats.presentation.overview.model

data class ConversationListItem(
    val conversationId: String,
    val contactId: String,
    val contactName: String,
    val lastMessage: String = "",
    val timestamp: String,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false
)
