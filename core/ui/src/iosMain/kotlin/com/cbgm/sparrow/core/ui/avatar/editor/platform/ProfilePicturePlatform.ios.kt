package com.cbgm.sparrow.core.ui.avatar.editor.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImagePickerControllerSourceType

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberProfilePictureSourceLauncher(
    onPictureSelected: (ByteArray) -> Unit,
    onSourceDismissed: () -> Unit
): ProfilePictureSourceLauncher {
    val viewController = LocalUIViewController.current
    val currentOnPictureSelected = rememberUpdatedState(onPictureSelected)
    val currentOnSourceDismissed = rememberUpdatedState(onSourceDismissed)
    val delegate =
        remember {
            ProfilePicturePickerDelegate(
                onPictureSelected = { bytes -> currentOnPictureSelected.value(bytes) },
                onDismissed = { currentOnSourceDismissed.value() }
            )
        }

    return remember(viewController, delegate) {
        ProfilePictureSourceLauncher(
            launchCamera = {
                if (
                    !presentProfilePicturePicker(
                        presentingViewController = viewController,
                        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                        delegate = delegate
                    )
                ) {
                    currentOnSourceDismissed.value()
                }
            },
            launchGallery = {
                if (
                    !presentProfilePicturePicker(
                        presentingViewController = viewController,
                        sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
                        delegate = delegate
                    )
                ) {
                    currentOnSourceDismissed.value()
                }
            }
        )
    }
}
