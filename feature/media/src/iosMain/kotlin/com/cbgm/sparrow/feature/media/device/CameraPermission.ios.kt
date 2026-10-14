package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberCameraPermissionRequest(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit {
    val currentOnDenied = rememberUpdatedState(onPermissionDenied)
    return remember { { currentOnDenied.value() } }
}
