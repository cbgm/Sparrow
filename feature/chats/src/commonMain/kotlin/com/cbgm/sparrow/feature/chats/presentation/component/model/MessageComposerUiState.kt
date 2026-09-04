package com.cbgm.sparrow.feature.chats.presentation.component.model

import com.cbgm.sparrow.feature.chats.domain.model.LocationShareState
import com.cbgm.sparrow.feature.chats.domain.model.MessageComposerAvailability
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection

data class MessageComposerUiState(
    val messageText: String = "",
    val replyTo: MessageReplyUi? = null,
    val editingMessageId: String? = null,
    val selectedMedia: List<MediaSelection> = emptyList(),
    val isSending: Boolean = false,
    val locationShareState: LocationShareState = LocationShareState.IDLE,
    val availability: MessageComposerAvailability =
        MessageComposerAvailability(
            isInputEnabled = false,
            isSendEnabled = false,
            canAddAttachment = false
        )
)
