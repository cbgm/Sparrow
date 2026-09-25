package com.cbgm.sparrow.feature.avatar.domain.model

import androidx.compose.ui.graphics.ImageBitmap

data class AvatarImage(
    val image: ImageBitmap?,
    val changedAtEpochMilliseconds: Long
)
