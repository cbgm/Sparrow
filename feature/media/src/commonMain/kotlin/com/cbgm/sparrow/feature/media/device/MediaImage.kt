package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
internal expect fun MediaImage(
    data: ByteArray?,
    localFilePath: String?,
    cacheKey: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
)
