package com.cbgm.securechat.feature.chats.presentation.group.model

sealed interface GroupUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : GroupUiEvent

    data object SendClicked : GroupUiEvent

    data object HeaderClicked : GroupUiEvent

    data class RetryMessage(
        val messageId: String
    ) : GroupUiEvent

    data object BackClicked : GroupUiEvent

    data object AcceptInvitation : GroupUiEvent

    data object DeclineInvitation : GroupUiEvent
}
