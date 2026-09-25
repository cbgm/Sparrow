package com.cbgm.sparrow.feature.chats.presentation.common.header.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

internal data class SecurityBannerState(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val containerColor: Color,
    val contentColor: Color
)
