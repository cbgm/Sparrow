package com.cbgm.sparrow.feature.chats.presentation.attachment.platform

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.chats.presentation.attachment.model.GalleryMediaSelection

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
