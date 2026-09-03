package com.cbgm.sparrow.feature.chats.presentation.direct.model

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

sealed interface DirectUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : DirectUiEvent

    data object SendClicked : DirectUiEvent

    data class ReplyToMessage(
        val messageId: String
    ) : DirectUiEvent

    data object CancelReply : DirectUiEvent

    data class EditMessage(
        val messageId: String
    ) : DirectUiEvent

    data object CancelEdit : DirectUiEvent

    data class MessageContextRequested(
        val messageId: String
    ) : DirectUiEvent

    data object MessageContextDismissed : DirectUiEvent

    data class MessageReactionSelected(
        val messageId: String,
        val emoji: String
    ) : DirectUiEvent

    data class DeleteMessage(
        val messageId: String
    ) : DirectUiEvent

    data class MediaSelected(
        val media: List<MediaSelection>
    ) : DirectUiEvent

    data class OpenFilePicker(
        val sessionId: String
    ) : DirectUiEvent

    data class ShareCurrentLocation(
        val location: CurrentLocation
    ) : DirectUiEvent

    data class ShareContact(
        val contact: SharedContact
    ) : DirectUiEvent

    data class AddSharedContact(
        val contact: SharedContact
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
