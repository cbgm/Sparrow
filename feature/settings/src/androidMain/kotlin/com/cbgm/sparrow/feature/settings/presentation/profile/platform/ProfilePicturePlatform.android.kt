package com.cbgm.sparrow.feature.settings.presentation.profile.platform

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
    onPictureSelected: (ByteArray) -> Unit
): ProfilePictureSourceLauncher {
    val context = LocalContext.current
    val currentOnPictureSelected by rememberUpdatedState(onPictureSelected)
    var showCamera by remember { mutableStateOf(false) }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            decodeProfilePictureBitmap(
                context = context,
                uri = uri
            )?.let(::encodeProfilePictureSource)?.let(currentOnPictureSelected)
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showCamera = true
            }
        }

    if (showCamera) {
        ProfilePictureCameraScreen(
            onPictureCaptured = { bytes ->
                showCamera = false
                currentOnPictureSelected(bytes)
            },
            onDismiss = { showCamera = false }
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
