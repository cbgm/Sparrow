package com.cbgm.securechat.feature.chats.presentation.model

sealed interface ChatUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : ChatUiEvent

    data object SendClicked : ChatUiEvent

    data object HeaderClicked : ChatUiEvent

    data class RetryMessage(
        val messageId: String
    ) : ChatUiEvent

    data object VerifyIdentityClicked : ChatUiEvent

    data object ManualIdentitySetupClicked : ChatUiEvent

    data object ShareIdentityClicked : ChatUiEvent

    data object ImportIdentityClicked : ChatUiEvent

    data object BackClicked : ChatUiEvent

    data object AcceptGroupInvitation : ChatUiEvent

    data object DeclineGroupInvitation : ChatUiEvent
}
