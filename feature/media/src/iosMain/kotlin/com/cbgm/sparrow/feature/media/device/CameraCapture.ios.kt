package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CameraLens
import com.cbgm.sparrow.feature.media.domain.model.CapturedMedia
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraCaptureMode
import platform.UIKit.UIImagePickerControllerCameraDevice
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerMediaURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCameraCaptureLauncher(
    config: CameraCaptureConfig,
    onCaptured: (CapturedMedia) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): CameraCaptureLauncher {
    val viewController = LocalUIViewController.current
    val currentOnCaptured = rememberUpdatedState(onCaptured)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    val currentOnError = rememberUpdatedState(onError)
    val delegate =
        remember(config) {
            CameraPickerDelegate(
                config = config,
                onCaptured = { currentOnCaptured.value(it) },
                onDismissed = { currentOnDismissed.value() },
                onError = { currentOnError.value(it) }
            )
        }

    return remember(viewController, delegate, config) {
        CameraCaptureLauncher(
            launch = {
                val sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                if (UIImagePickerController.isSourceTypeAvailable(sourceType)) {
                    val picker = UIImagePickerController()
                    picker.setSourceType(sourceType)
                    picker.setDelegate(delegate)
                    picker.setAllowsEditing(false)
                    picker.setShowsCameraControls(true)
                    picker.setMediaTypes(
                        config.allowedTypes.map { type ->
                            when (type) {
                                CameraCaptureType.PHOTO -> IMAGE_TYPE_IDENTIFIER
                                CameraCaptureType.VIDEO -> VIDEO_TYPE_IDENTIFIER
                            }
                        }
                    )
                    picker.setCameraCaptureMode(
                        when (config.initialType) {
                            CameraCaptureType.PHOTO ->
                                UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto
                            CameraCaptureType.VIDEO ->
                                UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModeVideo
                        }
                    )
                    picker.setCameraDevice(
                        when (config.initialLens) {
                            CameraLens.FRONT ->
                                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceFront
                            CameraLens.BACK ->
                                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear
                        }
                    )

                    viewController.presentViewController(picker, true, null)
                } else {
                    currentOnError.value("Camera is not available")
                }
            }
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class CameraPickerDelegate(
    private val config: CameraCaptureConfig,
    private val onCaptured: (CapturedMedia) -> Unit,
    private val onDismissed: () -> Unit,
    private val onError: (String) -> Unit
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val captured =
            runCatching {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                if (image != null) {
                    return@runCatching image.toCapturedPhoto(config)
                }

                val videoUrl = didFinishPickingMediaWithInfo[UIImagePickerControllerMediaURL] as? NSURL
                    ?: error("Captured media could not be read")
                videoUrl.toCapturedVideo(config)
            }

        picker.dismissViewControllerAnimated(true, null)
        captured
            .onSuccess(onCaptured)
            .onFailure { error -> onError(error.message ?: "Captured media could not be read") }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onDismissed()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toCapturedPhoto(config: CameraCaptureConfig): CapturedMedia {
    val sourceWidth = size.width
    val sourceHeight = size.height
    require(sourceWidth > 0.0 && sourceHeight > 0.0) { "Captured photo has invalid dimensions" }

    val maxDimension = config.maxImageDimension?.toDouble()
    val longestSide = max(sourceWidth, sourceHeight)
    val scale =
        if (maxDimension != null && longestSide > maxDimension) {
            maxDimension / longestSide
        } else {
            1.0
        }
    val targetWidth = (sourceWidth * scale).coerceAtLeast(1.0)
    val targetHeight = (sourceHeight * scale).coerceAtLeast(1.0)

    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(targetWidth, targetHeight),
        true,
        1.0
    )
    val normalized =
        try {
            drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
            requireNotNull(UIGraphicsGetImageFromCurrentImageContext()) {
                "Captured photo could not be normalized"
            }
        } finally {
            UIGraphicsEndImageContext()
        }

    val bytes = normalized.encodeCameraPhoto(config.maxImageBytes)
    return CapturedMedia(
        type = CameraCaptureType.PHOTO,
        bytes = bytes,
        mimeType = "image/jpeg",
        width = targetWidth.roundToInt().coerceAtLeast(1),
        height = targetHeight.roundToInt().coerceAtLeast(1)
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.encodeCameraPhoto(maxBytes: Int?): ByteArray {
    for (quality in CAMERA_JPEG_QUALITIES) {
        val bytes = UIImageJPEGRepresentation(this, quality)?.toByteArray() ?: continue
        if (maxBytes == null || bytes.size <= maxBytes) return bytes
    }
    error("Captured photo exceeds the configured size limit")
}

@OptIn(ExperimentalForeignApi::class)
private fun NSURL.toCapturedVideo(config: CameraCaptureConfig): CapturedMedia {
    val data = NSData(contentsOfURL = this) ?: error("Captured video could not be read")
    config.maxVideoBytes?.let { maxBytes ->
        require(data.length <= maxBytes.toULong()) { "Captured video exceeds the configured size limit" }
    }
    return CapturedMedia(
        type = CameraCaptureType.VIDEO,
        bytes = data.toByteArray(),
        mimeType = "video/quicktime"
    )
}

private const val IMAGE_TYPE_IDENTIFIER = "public.image"
private const val VIDEO_TYPE_IDENTIFIER = "public.movie"
private val CAMERA_JPEG_QUALITIES = listOf(0.92, 0.88, 0.84, 0.80, 0.76, 0.72, 0.68, 0.64, 0.60)
