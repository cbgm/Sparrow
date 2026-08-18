package com.cbgm.sparrow.feature.chats.presentation.overview.mapper

import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.presentation.overview.formatter.formatConversationTimestamp
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItem
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiState

internal fun List<ConversationOverview>.directContactIds(): Set<String> =
    asSequence()
        .filter { conversation -> conversation.type == ConversationOverviewType.DIRECT }
        .map(ConversationOverview::contactId)
        .filter(String::isNotBlank)
        .toSet()

internal fun List<ConversationOverview>.toUiState(
    profilePictures: Map<String, ByteArray?>
): OverviewUiState =
    if (isEmpty()) {
        OverviewUiState.Empty
    } else {
        OverviewUiState.Content(
            conversations =
                map { conversation ->
                    conversation.toConversationListItem(
                        profilePictureBytes =
                            conversation
                                .takeIf { it.type == ConversationOverviewType.DIRECT }
                                ?.let { profilePictures[it.contactId] }
                    )
                }
        )
    }

internal fun ConversationOverview.toConversationListItem(
    profilePictureBytes: ByteArray? = null
): ConversationListItem =
    ConversationListItem(
        conversationId = id,
        contactId = contactId,
        contactName = displayName,
        profilePictureBytes = profilePictureBytes,
        lastMessage = lastMessageText.orEmpty(),
        timestamp = lastMessageTimestamp?.let(::formatConversationTimestamp).orEmpty(),
        unreadCount = unreadCount,
        isGroup = type == ConversationOverviewType.GROUP
    )
