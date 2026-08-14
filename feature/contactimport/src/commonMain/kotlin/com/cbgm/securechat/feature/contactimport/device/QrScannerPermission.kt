package com.cbgm.securechat.feature.contactimport.device

import androidx.compose.runtime.Composable

/**
 * Returns a callback that requests camera permission.
 *
 * When permission already exists, [onPermissionGranted] is called
 * immediately.
 */
@Composable
expect fun rememberQrScannerPermissionRequest(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit
