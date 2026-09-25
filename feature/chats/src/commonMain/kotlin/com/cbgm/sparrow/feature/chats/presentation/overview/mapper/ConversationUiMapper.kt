package com.cbgm.sparrow.feature.chats.presentation.overview.mapper

import com.cbgm.sparrow.core.time.formatMessageTimestamp
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewPreview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItem
import com.cbgm.sparrow.feature.chats.presentation.overview.model.LastMessagePreviewUi
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiState

internal fun List<ConversationOverview>.toOverviewUiState(
    activeAutoReplyName: String?,
    error: String? = null
): OverviewUiState =
    OverviewUiState(
        conversations = map(ConversationOverview::toConversationListItem),
        activeAutoReplyName = activeAutoReplyName,
        isLoading = false,
        error = error
    )

internal fun ConversationOverview.toConversationListItem(): ConversationListItem =
    ConversationListItem(
        conversationId = id,
        contactId = contactId,
        contactName = displayName,
        avatarTarget =
            when (type) {
                ConversationOverviewType.DIRECT -> AvatarTarget.User(contactId)
                ConversationOverviewType.GROUP -> AvatarTarget.Group(id)
            },
        lastMessage = lastMessageText.orEmpty(),
        hasMessages = lastMessageTimestamp != null,
        timestamp = lastMessageTimestamp?.let(::formatMessageTimestamp).orEmpty(),
        unreadCount = unreadCount,
        isGroup = type == ConversationOverviewType.GROUP,
        lastMessagePreview =
            when (lastMessagePreview) {
                ConversationOverviewPreview.MEDIA -> LastMessagePreviewUi.MEDIA
                ConversationOverviewPreview.LOCATION -> LastMessagePreviewUi.LOCATION
                ConversationOverviewPreview.CONTACT_CARD -> LastMessagePreviewUi.CONTACT_CARD
                ConversationOverviewPreview.VOICE -> LastMessagePreviewUi.VOICE
                ConversationOverviewPreview.ATTACHMENT -> LastMessagePreviewUi.ATTACHMENT
                null -> null
            }
    )
