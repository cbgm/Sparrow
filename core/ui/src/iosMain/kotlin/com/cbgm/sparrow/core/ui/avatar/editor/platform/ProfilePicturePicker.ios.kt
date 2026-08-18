package com.cbgm.sparrow.core.ui.avatar.editor.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal fun presentProfilePicturePicker(
    presentingViewController: UIViewController,
    sourceType: UIImagePickerControllerSourceType,
    delegate: ProfilePicturePickerDelegate
): Boolean {
    if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
        return false
    }

    val picker = UIImagePickerController()
    picker.setSourceType(sourceType)
    picker.setDelegate(delegate)
    picker.setAllowsEditing(false)

    presentingViewController.presentViewController(
        picker,
        true,
        null
    )
    return true
}

@OptIn(ExperimentalForeignApi::class)
internal class ProfilePicturePickerDelegate(
    private val onPictureSelected: (ByteArray) -> Unit,
    private val onDismissed: () -> Unit
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image =
            didFinishPickingMediaWithInfo[
                UIImagePickerControllerOriginalImage
            ] as? UIImage

        picker.dismissViewControllerAnimated(true, null)

        val bytes = image?.normalizedProfilePictureBytes()
        if (bytes != null) {
            onPictureSelected(bytes)
        } else {
            onDismissed()
        }
    }

    override fun imagePickerControllerDidCancel(
        picker: UIImagePickerController
    ) {
        picker.dismissViewControllerAnimated(true, null)
        onDismissed()
    }
}
