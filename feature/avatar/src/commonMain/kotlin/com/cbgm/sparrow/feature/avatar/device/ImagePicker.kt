package com.cbgm.sparrow.feature.avatar.device

import androidx.compose.runtime.Composable

data class ImagePickerLauncher(
    val launch: () -> Unit
)

@Composable
expect fun rememberImagePickerLauncher(
    onImageSelected: (ByteArray) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): ImagePickerLauncher
