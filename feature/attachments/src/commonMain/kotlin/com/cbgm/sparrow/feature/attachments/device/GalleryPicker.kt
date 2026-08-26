package com.cbgm.sparrow.feature.attachments.device

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.attachments.presentation.model.MediaSelection

data class GalleryPickerLauncher(
    val launch: () -> Unit
)

@Composable
expect fun rememberGalleryPickerLauncher(
    maxItems: Int,
    selectedMedia: List<MediaSelection>,
    onMediaSelected: (List<MediaSelection>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): GalleryPickerLauncher
