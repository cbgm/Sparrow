package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

sealed interface GroupConversationUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : GroupConversationUiEvent

    data object SendClicked : GroupConversationUiEvent

    data object LoadOlderMessages : GroupConversationUiEvent

    data class MessageHistoryTargetRequested(
        val messageId: String
    ) : GroupConversationUiEvent

    data class ReplyToMessage(
        val messageId: String
    ) : GroupConversationUiEvent

    data object CancelReply : GroupConversationUiEvent

    data class EditMessage(
        val messageId: String
    ) : GroupConversationUiEvent

    data object CancelEdit : GroupConversationUiEvent

    data class MessageContextRequested(
        val messageId: String
    ) : GroupConversationUiEvent

    data object MessageContextDismissed : GroupConversationUiEvent

    data class MessageReactionSelected(
        val messageId: String,
        val emoji: String
    ) : GroupConversationUiEvent

    data class DeleteMessage(
        val messageId: String
    ) : GroupConversationUiEvent

    data class ForwardMessage(
        val messageId: String,
        val target: ForwardingTarget
    ) : GroupConversationUiEvent

    data class MediaSelected(
        val media: List<MediaSelection>
    ) : GroupConversationUiEvent

    data class OpenFilePicker(
        val sessionId: String
    ) : GroupConversationUiEvent

    data object LocationCaptureStarted : GroupConversationUiEvent

    data class ShareCurrentLocation(
        val location: CurrentLocation
    ) : GroupConversationUiEvent

    data class LocationCaptureFailed(
        val message: String
    ) : GroupConversationUiEvent

    data class ShareContact(
        val contact: SharedContact
    ) : GroupConversationUiEvent

    data class AddSharedContact(
        val contact: SharedContact
    ) : GroupConversationUiEvent

    data class AttachmentVisible(
        val attachmentId: String
    ) : GroupConversationUiEvent

    data class AttachmentError(
        val message: String
    ) : GroupConversationUiEvent

    data object HeaderClicked : GroupConversationUiEvent

    data class RetryMessage(
        val messageId: String
    ) : GroupConversationUiEvent

    data class SafetyWarningClicked(
        val messageId: String,
        val contactId: String?,
        val warning: MessageSafetyWarningUi
    ) : GroupConversationUiEvent

    data object BackClicked : GroupConversationUiEvent

    data object AcceptInvitation : GroupConversationUiEvent

    data object DeclineInvitation : GroupConversationUiEvent
}
