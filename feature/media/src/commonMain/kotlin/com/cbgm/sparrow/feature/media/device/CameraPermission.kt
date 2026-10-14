package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable

/**
 * Returns a callback that requests camera permission.
 *
 * When permission already exists, [onPermissionGranted] is called
 * immediately.
 */
@Composable
expect fun rememberCameraPermissionRequest(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit
