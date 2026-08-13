package com.cbgm.securechat.feature.chats.presentation.overview.mapper

import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.presentation.overview.model.ConversationListItem

fun Conversation.toConversationListItem() =
    ConversationListItem(
        conversationId = id,
        contactId = contactId,
        contactName = contactName,
        lastMessage = lastMessage?.text ?: "",
        timestamp = lastMessage?.timestamp?.toString().orEmpty(),
        unreadCount = unreadCount,
        isGroup = isGroup
    )
