package com.cbgm.securechat.feature.chats.presentation.overview.model

sealed interface OverviewUiEvent {
    data class ChatClicked(
        val chat: ConversationListItem
    ) : OverviewUiEvent

    data class DeleteConversation(
        val conversationId: String
    ) : OverviewUiEvent
}
