package com.cbgm.sparrow.feature.chats.presentation.direct.model

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

sealed interface DirectUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : DirectUiEvent

    data object SendClicked : DirectUiEvent

    data class MediaSelected(
        val media: List<MediaSelection>
    ) : DirectUiEvent

    data class OpenFilePicker(
        val sessionId: String
    ) : DirectUiEvent

    data class ShareCurrentLocation(
        val location: CurrentLocation
    ) : DirectUiEvent

    data class AttachmentVisible(
        val attachmentId: String
    ) : DirectUiEvent

    data class AttachmentError(
        val message: String
    ) : DirectUiEvent

    data object HeaderClicked : DirectUiEvent

    data class RetryMessage(
        val messageId: String
    ) : DirectUiEvent

    data class SafetyWarningClicked(
        val messageId: String,
        val warning: MessageSafetyWarningUi
    ) : DirectUiEvent

    data object VerifyIdentityClicked : DirectUiEvent

    data object ShareIdentityClicked : DirectUiEvent

    data object ImportIdentityClicked : DirectUiEvent

    data object BackClicked : DirectUiEvent
}
