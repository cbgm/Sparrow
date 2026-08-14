package com.cbgm.sparrow.feature.chats.data.overview.mapper

import com.cbgm.sparrow.data.database.model.ConversationSummary
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType

internal fun ConversationSummary.toConversationOverview(): ConversationOverview {
    val type =
        if (conversationType == GROUP_CONVERSATION_TYPE) {
            ConversationOverviewType.GROUP
        } else {
            ConversationOverviewType.DIRECT
        }
    val displayName =
        if (type == ConversationOverviewType.GROUP) {
            conversationTitle.orEmpty()
        } else {
            contactName?.takeIf(String::isNotBlank) ?: "Unknown contact"
        }

    return ConversationOverview(
        id = conversationId,
        contactId = contactId.orEmpty(),
        displayName = displayName,
        lastMessageText = lastMessageText,
        lastMessageTimestamp = lastMessageTimestamp,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
        unreadCount = unreadCount,
        participantCount = participantCount,
        type = type
    )
}

private const val GROUP_CONVERSATION_TYPE = "GROUP"
