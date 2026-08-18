package com.cbgm.sparrow.core.ui.avatar.editor.platform

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal actual fun rememberProfilePictureSourceLauncher(
    onPictureSelected: (ByteArray) -> Unit,
    onSourceDismissed: () -> Unit
): ProfilePictureSourceLauncher {
    val context = LocalContext.current
    val currentOnPictureSelected by rememberUpdatedState(onPictureSelected)
    val currentOnSourceDismissed by rememberUpdatedState(onSourceDismissed)
    var showCamera by remember { mutableStateOf(false) }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) {
                currentOnSourceDismissed()
                return@rememberLauncherForActivityResult
            }

            val bytes =
                decodeProfilePictureBitmap(
                    context = context,
                    uri = uri
                )?.let(::encodeProfilePictureSource)

            if (bytes != null) {
                currentOnPictureSelected(bytes)
            } else {
                currentOnSourceDismissed()
            }
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showCamera = true
            } else {
                currentOnSourceDismissed()
            }
        }

    if (showCamera) {
        ProfilePictureCameraScreen(
            onPictureCaptured = { bytes ->
                showCamera = false
                currentOnPictureSelected(bytes)
            },
            onDismiss = {
                showCamera = false
                currentOnSourceDismissed()
            }
        )
    }

    return remember(context, galleryLauncher, cameraPermissionLauncher) {
        ProfilePictureSourceLauncher(
            launchCamera = {
                if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    showCamera = true
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            launchGallery = {
                galleryLauncher.launch("image/*")
            }
        )
    }
}
