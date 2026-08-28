package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.presentation.details.model.MessageSafetyWarningUiModel

sealed interface GroupUiEvent {
    data class MessageTextChanged(
        val text: String
    ) : GroupUiEvent

    data object SendClicked : GroupUiEvent

    data class MediaSelected(
        val media: List<MediaSelection>
    ) : GroupUiEvent

    data object OpenFilePickerClicked : GroupUiEvent

    data class ShareCurrentLocation(
        val location: CurrentLocation
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
        val warning: MessageSafetyWarningUiModel
    ) : GroupUiEvent

    data object BackClicked : GroupUiEvent

    data object AcceptInvitation : GroupUiEvent

    data object DeclineInvitation : GroupUiEvent
}
