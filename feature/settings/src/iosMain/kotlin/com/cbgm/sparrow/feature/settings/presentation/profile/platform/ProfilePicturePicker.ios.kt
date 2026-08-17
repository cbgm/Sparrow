package com.cbgm.sparrow.feature.settings.presentation.profile.platform

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
) {
    if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
        return
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
}

@OptIn(ExperimentalForeignApi::class)
internal class ProfilePicturePickerDelegate(
    private val onPictureSelected: (ByteArray) -> Unit
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

        image
            ?.normalizedProfilePictureBytes()
            ?.let(onPictureSelected)
    }

    override fun imagePickerControllerDidCancel(
        picker: UIImagePickerController
    ) {
        picker.dismissViewControllerAnimated(true, null)
    }
}
