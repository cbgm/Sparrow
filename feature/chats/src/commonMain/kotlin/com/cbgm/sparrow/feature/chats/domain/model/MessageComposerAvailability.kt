package com.cbgm.sparrow.feature.chats.domain.model

import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy

data class MessageComposerAvailability(
    val isInputEnabled: Boolean,
    val isSendEnabled: Boolean,
    val canAddAttachment: Boolean
)

object MessageComposerPolicy {
    fun resolve(
        isInputAllowed: Boolean,
        isSendAllowed: Boolean,
        isSending: Boolean,
        isEditing: Boolean,
        selectedAttachmentCount: Int,
        locationShareState: LocationShareState
    ): MessageComposerAvailability {
        val isLocationInProgress = locationShareState != LocationShareState.IDLE
        val isInputEnabled = isInputAllowed && !isSending
        val isSendEnabled = isSendAllowed && !isSending && !isLocationInProgress
        val canAddAttachment =
            isInputEnabled &&
                !isLocationInProgress &&
                !isEditing &&
                selectedAttachmentCount < MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE

        return MessageComposerAvailability(
            isInputEnabled = isInputEnabled,
            isSendEnabled = isSendEnabled,
            canAddAttachment = canAddAttachment
        )
    }
}
