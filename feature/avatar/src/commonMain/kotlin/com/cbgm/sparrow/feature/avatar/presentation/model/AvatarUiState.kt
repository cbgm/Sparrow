package com.cbgm.sparrow.feature.avatar.presentation.model

import androidx.compose.ui.graphics.ImageBitmap

sealed interface AvatarUiState {
    data object Loading : AvatarUiState

    data object Empty : AvatarUiState

    data class Ready(
        val image: ImageBitmap,
        val changedAtEpochMilliseconds: Long
    ) : AvatarUiState

    data class Error(
        val throwable: Throwable
    ) : AvatarUiState
}
