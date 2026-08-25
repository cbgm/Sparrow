package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem

@Composable
internal expect fun VideoThumbnail(
    media: MediaItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
)

@Composable
internal expect fun VideoPlayer(
    media: MediaItem,
    isActive: Boolean,
    modifier: Modifier = Modifier
)
