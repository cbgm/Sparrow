package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUi

sealed interface GroupUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : GroupUiEvent

    data object SendClicked : GroupUiEvent

    data class ReplyToMessage(
        val messageId: String
    ) : GroupUiEvent

    data object CancelReply : GroupUiEvent

    data class MessageReactionSelected(
        val messageId: String,
        val emoji: String
    ) : GroupUiEvent

    data class MediaSelected(
        val media: List<MediaSelection>
    ) : GroupUiEvent

    data class OpenFilePicker(
        val sessionId: String
    ) : GroupUiEvent

    data class ShareCurrentLocation(
        val location: CurrentLocation
    ) : GroupUiEvent

    data class ShareContact(
        val contact: SharedContact
    ) : GroupUiEvent

    data class AddSharedContact(
        val contact: SharedContact
    ) : GroupUiEvent

    data class AttachmentVisible(
        val attachmentId: String
    ) : GroupUiEvent

    data class AttachmentError(
        val message: String
    ) : GroupUiEvent

    data object HeaderClicked : GroupUiEvent

    data class RetryMessage(
        val messageId: String
    ) : GroupUiEvent

    data class SafetyWarningClicked(
        val messageId: String,
        val contactId: String?,
        val warning: MessageSafetyWarningUi
    ) : GroupUiEvent

    data object BackClicked : GroupUiEvent

    data object AcceptInvitation : GroupUiEvent

    data object DeclineInvitation : GroupUiEvent
}
