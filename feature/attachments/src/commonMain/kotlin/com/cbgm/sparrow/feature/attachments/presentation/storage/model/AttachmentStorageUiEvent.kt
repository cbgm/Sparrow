package com.cbgm.sparrow.feature.attachments.presentation.storage.model

sealed interface AttachmentStorageUiEvent {
    data object BackClicked : AttachmentStorageUiEvent

    data class ConversationClicked(
        val conversationId: String
    ) : AttachmentStorageUiEvent
}
