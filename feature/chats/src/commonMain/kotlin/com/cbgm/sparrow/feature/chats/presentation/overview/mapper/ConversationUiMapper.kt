package com.cbgm.sparrow.feature.chats.presentation.overview.mapper

import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.presentation.overview.formatter.formatConversationTimestamp
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItem

internal fun ConversationOverview.toConversationListItem(): ConversationListItem =
    ConversationListItem(
        conversationId = id,
        contactId = contactId,
        contactName = displayName,
        lastMessage = lastMessageText.orEmpty(),
        timestamp = lastMessageTimestamp?.let(::formatConversationTimestamp).orEmpty(),
        unreadCount = unreadCount,
        isGroup = type == ConversationOverviewType.GROUP
    )
