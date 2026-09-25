package com.cbgm.sparrow.feature.linkpreview.presentation.model

sealed interface LinkPreviewUiState {
    data object Loading : LinkPreviewUiState

    data class Success(
        val preview: LinkPreviewUi
    ) : LinkPreviewUiState

    data class Error(
        val throwable: Throwable
    ) : LinkPreviewUiState
}
