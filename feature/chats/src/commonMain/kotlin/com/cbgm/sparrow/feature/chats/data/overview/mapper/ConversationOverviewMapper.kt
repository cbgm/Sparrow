package com.cbgm.sparrow.feature.chats.data.overview.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.entity.ConversationType
import com.cbgm.sparrow.data.database.model.ConversationSummaryDto
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewPreview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType

internal fun ConversationSummaryDto.toConversationOverview(): ConversationOverview {
    val type =
        if (conversationType == ConversationType.GROUP.name) {
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
        type = type,
        lastMessagePreview =
            when (lastMessageAttachmentType) {
                null -> null
                MessageAttachmentType.IMAGE,
                MessageAttachmentType.VIDEO,
                MessageAttachmentType.FILE -> ConversationOverviewPreview.MEDIA
                MessageAttachmentType.LOCATION -> ConversationOverviewPreview.LOCATION
                MessageAttachmentType.CONTACT -> ConversationOverviewPreview.CONTACT_CARD
                MessageAttachmentType.VOICE -> ConversationOverviewPreview.VOICE
            }
    )
}
