package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.safety.presentation.model.MessageSafetyWarningUiModel

sealed interface GroupUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : GroupUiEvent

    data object SendClicked : GroupUiEvent

    data object HeaderClicked : GroupUiEvent

    data class RetryMessage(
        val messageId: String
    ) : GroupUiEvent

    data class SafetyWarningClicked(
        val messageId: String,
        val contactId: String?,
        val warning: MessageSafetyWarningUiModel
    ) : GroupUiEvent

    data object BackClicked : GroupUiEvent

    data object AcceptInvitation : GroupUiEvent

    data object DeclineInvitation : GroupUiEvent
}
