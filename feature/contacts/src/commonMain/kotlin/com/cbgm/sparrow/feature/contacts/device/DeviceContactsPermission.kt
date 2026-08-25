package com.cbgm.sparrow.feature.contacts.device

import androidx.compose.runtime.Composable

/**
 * Returns a callback that requests access to the device contacts.
 *
 * If permission is already granted, [onPermissionGranted] is
 * executed immediately.
 */
@Composable
expect fun rememberDeviceContactsPermissionRequest(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
): () -> Unit
