package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (ByteArray) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): ImagePickerLauncher {
    val viewController = LocalUIViewController.current
    val currentOnImageSelected = rememberUpdatedState(onImageSelected)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    val currentOnError = rememberUpdatedState(onError)
    val delegate =
        remember {
            ImagePickerDelegate(
                onImageSelected = { currentOnImageSelected.value(it) },
                onDismissed = { currentOnDismissed.value() },
                onError = { currentOnError.value(it) }
            )
        }

    return remember(viewController, delegate) {
        ImagePickerLauncher(
            launch = {
                if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)) {
                    val picker = UIImagePickerController()
                    picker.setSourceType(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
                    picker.setDelegate(delegate)
                    picker.setAllowsEditing(false)
                    viewController.presentViewController(picker, true, null)
                } else {
                    currentOnError.value("Photo library is not available")
                }
            }
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onImageSelected: (ByteArray) -> Unit,
    private val onDismissed: () -> Unit,
    private val onError: (String) -> Unit
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, null)
        val bytes = image?.normalizedProfilePictureBytes()
        if (bytes != null) {
            onImageSelected(bytes)
        } else {
            onError("Selected image could not be read")
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onDismissed()
    }
}
