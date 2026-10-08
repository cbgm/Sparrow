package com.cbgm.sparrow.feature.attachments.presentation.model

sealed interface AttachmentUiState<out T> {
    data object Idle : AttachmentUiState<Nothing>

    data object Loading : AttachmentUiState<Nothing>

    data class Ready<T>(
        val value: T
    ) : AttachmentUiState<T>

    data class Error(
        val throwable: Throwable
    ) : AttachmentUiState<Nothing>
}
