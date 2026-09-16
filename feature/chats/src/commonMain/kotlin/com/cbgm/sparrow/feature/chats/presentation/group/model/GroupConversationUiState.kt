package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupComposerState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationState

data class GroupConversationUiState(
    val groupId: String = "",
    val title: String = "",
    val messages: List<MessageBubbleUi> = emptyList(),
    val pinnedMessage: MessageBubbleUi? = null,
    val pinnedAtEpochMilliseconds: Long = 0L,
    val isLocalAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val state: GroupConversationState = GroupConversationState.READY,
    val composerState: GroupComposerState = GroupComposerState.DISABLED
)

fun GroupConversationUiState.findMessage(id: String?) =
    messages.firstOrNull { it.id == id } ?: pinnedMessage?.takeIf { it.id == id }
