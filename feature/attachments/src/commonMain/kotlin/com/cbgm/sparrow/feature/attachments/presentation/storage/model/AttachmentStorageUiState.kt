package com.cbgm.sparrow.feature.attachments.presentation.storage.model

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary

sealed interface AttachmentStorageUiState {
    data object Loading : AttachmentStorageUiState

    data class Content(
        val conversations: List<AttachmentStorageSummary>
    ) : AttachmentStorageUiState

    data class Error(
        val message: String
    ) : AttachmentStorageUiState
}
