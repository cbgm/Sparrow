package com.cbgm.sparrow.feature.chats.presentation.direct.model

import com.cbgm.sparrow.feature.chats.presentation.attachment.model.GalleryMediaSelection
import com.cbgm.sparrow.feature.safety.presentation.model.MessageSafetyWarningUiModel

sealed interface DirectUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : DirectUiEvent

    data object SendClicked : DirectUiEvent

    data class GalleryMediaSelected(
        val media: List<GalleryMediaSelection>
    ) : DirectUiEvent

    data class MediaAttachmentVisible(
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
        val warning: MessageSafetyWarningUiModel
    ) : DirectUiEvent

    data object VerifyIdentityClicked : DirectUiEvent

    data object ShareIdentityClicked : DirectUiEvent

    data object ImportIdentityClicked : DirectUiEvent

    data object BackClicked : DirectUiEvent
}
