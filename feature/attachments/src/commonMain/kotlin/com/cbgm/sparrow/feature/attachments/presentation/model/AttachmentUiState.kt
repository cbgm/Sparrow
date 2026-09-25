package com.cbgm.sparrow.feature.attachments.presentation.model

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent

sealed interface AttachmentUiState {
    data object Idle : AttachmentUiState

    data object Loading : AttachmentUiState

    data class Ready(
        val content: AttachmentContent
    ) : AttachmentUiState

    data class Error(
        val throwable: Throwable
    ) : AttachmentUiState
}
