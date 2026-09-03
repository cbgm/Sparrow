package com.cbgm.sparrow.feature.chats.presentation.component.model

import androidx.compose.ui.graphics.vector.ImageVector

data class ComposerPreviewUi(
    val iconText: String,
    val icon: ImageVector,
    val additionalText: String = "",
    val type: Type
) {
    enum class Type {
        REPLY,
        EDIT
    }
}
