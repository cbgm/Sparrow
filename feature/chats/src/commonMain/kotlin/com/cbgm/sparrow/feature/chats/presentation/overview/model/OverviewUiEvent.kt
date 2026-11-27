package com.cbgm.sparrow.feature.chats.presentation.overview.model

sealed interface OverviewUiEvent {
    data object AutoReplyClicked : OverviewUiEvent

    data class ChatClicked(
        val chat: ConversationListItemUi
    ) : OverviewUiEvent

    data class DeleteConversation(
        val conversationId: String
    ) : OverviewUiEvent

    data object ErrorDismissed : OverviewUiEvent
}
