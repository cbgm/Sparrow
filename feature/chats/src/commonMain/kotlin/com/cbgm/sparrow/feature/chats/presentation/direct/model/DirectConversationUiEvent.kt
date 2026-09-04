package com.cbgm.sparrow.feature.chats.presentation.direct.model

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

sealed interface DirectConversationUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : DirectConversationUiEvent

    data object SendClicked : DirectConversationUiEvent

    data class ReplyToMessage(
        val messageId: String
    ) : DirectConversationUiEvent

    data object CancelReply : DirectConversationUiEvent

    data class EditMessage(
        val messageId: String
    ) : DirectConversationUiEvent

    data object CancelEdit : DirectConversationUiEvent

    data class MessageContextRequested(
        val messageId: String
    ) : DirectConversationUiEvent

    data object MessageContextDismissed : DirectConversationUiEvent

    data class MessageReactionSelected(
        val messageId: String,
        val emoji: String
    ) : DirectConversationUiEvent

    data class DeleteMessage(
        val messageId: String
    ) : DirectConversationUiEvent

    data class MediaSelected(
        val media: List<MediaSelection>
    ) : DirectConversationUiEvent

    data class OpenFilePicker(
        val sessionId: String
    ) : DirectConversationUiEvent

    data object LocationCaptureStarted : DirectConversationUiEvent

    data class ShareCurrentLocation(
        val location: CurrentLocation
    ) : DirectConversationUiEvent

    data class LocationCaptureFailed(
        val message: String
    ) : DirectConversationUiEvent

    data class ShareContact(
        val contact: SharedContact
    ) : DirectConversationUiEvent

    data class AddSharedContact(
        val contact: SharedContact
    ) : DirectConversationUiEvent

    data class AttachmentVisible(
        val attachmentId: String
    ) : DirectConversationUiEvent

    data class AttachmentError(
        val message: String
    ) : DirectConversationUiEvent

    data object HeaderClicked : DirectConversationUiEvent

    data class RetryMessage(
        val messageId: String
    ) : DirectConversationUiEvent

    data class SafetyWarningClicked(
        val messageId: String,
        val warning: MessageSafetyWarningUi
    ) : DirectConversationUiEvent

    data object VerifyIdentityClicked : DirectConversationUiEvent

    data object ShareIdentityClicked : DirectConversationUiEvent

    data object ImportIdentityClicked : DirectConversationUiEvent

    data object BackClicked : DirectConversationUiEvent
}
