package com.cbgm.sparrow.feature.avatar.presentation.editor.model

import androidx.compose.ui.graphics.ImageBitmap

data class AvatarEditorUiState(
    val image: ImageBitmap? = null,
    val isPreparing: Boolean = false,
    val isCropping: Boolean = false,
    val error: Throwable? = null
)
