package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.media.domain.model.GalleryMedia
import com.cbgm.sparrow.feature.media.domain.model.GalleryPickerConfig

data class GalleryPickerLauncher(
    val launch: () -> Unit
)

data class GalleryPickerStrings(
    val title: String,
    val closeContentDescription: String
)

@Composable
expect fun rememberGalleryPickerLauncher(
    config: GalleryPickerConfig,
    selectedMedia: List<GalleryMedia>,
    strings: GalleryPickerStrings,
    onMediaSelected: (List<GalleryMedia>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): GalleryPickerLauncher
