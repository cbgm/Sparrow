package com.cbgm.sparrow.feature.chats.presentation.common.composer.model

import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageReplyUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi

data class MessageComposerUiState(
    val messageText: String = "",
    val replyTo: MessageReplyUi? = null,
    val editingMessageId: String? = null,
    val selectedMedia: List<MediaSelectionUi> = emptyList(),
    val isSending: Boolean = false,
    val isLocationInProgress: Boolean = false,
    val availability: ComposerAvailabilityUi =
        ComposerAvailabilityUi(
            isInputEnabled = false,
            isSendEnabled = false,
            canAddAttachment = false
        )
)
