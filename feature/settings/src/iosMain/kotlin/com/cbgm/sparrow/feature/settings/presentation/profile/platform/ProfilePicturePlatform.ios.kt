package com.cbgm.sparrow.feature.settings.presentation.profile.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImagePickerControllerSourceType

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberProfilePictureSourceLauncher(
    onPictureSelected: (ByteArray) -> Unit
): ProfilePictureSourceLauncher {
    val viewController = LocalUIViewController.current
    val currentOnPictureSelected = rememberUpdatedState(onPictureSelected)
    val delegate =
        remember {
            ProfilePicturePickerDelegate { bytes ->
                currentOnPictureSelected.value(bytes)
            }
        }

    return remember(viewController, delegate) {
        ProfilePictureSourceLauncher(
            launchCamera = {
                presentProfilePicturePicker(
                    presentingViewController = viewController,
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                    delegate = delegate
                )
            },
            launchGallery = {
                presentProfilePicturePicker(
                    presentingViewController = viewController,
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
                    delegate = delegate
                )
            }
        )
    }
}
