package com.cbgm.securechat.feature.chats.presentation.overview.mapper

import com.cbgm.securechat.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.securechat.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.securechat.feature.chats.presentation.overview.model.ConversationListItem

internal fun ConversationOverview.toConversationListItem(): ConversationListItem =
    ConversationListItem(
        conversationId = id,
        contactId = contactId,
        contactName = displayName,
        lastMessage = lastMessageText.orEmpty(),
        timestamp = lastMessageTimestamp?.toString().orEmpty(),
        unreadCount = unreadCount,
        isGroup = type == ConversationOverviewType.GROUP
    )
