package com.cbgm.sparrow.feature.chats.presentation.overview.mapper

import com.cbgm.sparrow.core.time.formatMessageTimestamp
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItem
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiState

internal fun List<ConversationOverview>.toOverviewUiState(
    profilePictures: Map<String, ByteArray?>,
    groupAvatars: Map<String, ByteArray?>
): OverviewUiState =
    if (isEmpty()) {
        OverviewUiState.Empty
    } else {
        OverviewUiState.Content(
            conversations =
                map { conversation ->
                    conversation.toConversationListItem(
                        avatarBytes =
                            when (conversation.type) {
                                ConversationOverviewType.DIRECT -> profilePictures[conversation.contactId]
                                ConversationOverviewType.GROUP -> groupAvatars[conversation.id]
                            }
                    )
                }
        )
    }

internal fun ConversationOverview.toConversationListItem(
    avatarBytes: ByteArray? = null
): ConversationListItem =
    ConversationListItem(
        conversationId = id,
        contactId = contactId,
        contactName = displayName,
        avatarBytes = avatarBytes,
        lastMessage = lastMessageText.orEmpty(),
        timestamp = lastMessageTimestamp?.let(::formatMessageTimestamp).orEmpty(),
        unreadCount = unreadCount,
        isGroup = type == ConversationOverviewType.GROUP
    )
