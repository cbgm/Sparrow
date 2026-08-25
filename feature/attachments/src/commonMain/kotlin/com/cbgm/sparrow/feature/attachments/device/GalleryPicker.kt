package com.cbgm.sparrow.feature.attachments.device

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.attachments.presentation.model.GalleryMediaSelection

data class GalleryPickerLauncher(
    val launch: () -> Unit
)

@Composable
expect fun rememberGalleryPickerLauncher(
    maxItems: Int,
    selectedMedia: List<GalleryMediaSelection>,
    onMediaSelected: (List<GalleryMediaSelection>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): GalleryPickerLauncher
