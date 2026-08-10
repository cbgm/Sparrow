package com.cbgm.securechat.feature.chats.presentation.model

sealed interface ChatsUiEvent {
    data class ChatClicked(
        val chat: ChatListItem
    ) : ChatsUiEvent

    data class DeleteConversation(
        val conversationId: String
    ) : ChatsUiEvent
}
