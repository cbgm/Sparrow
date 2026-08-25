package com.cbgm.sparrow.feature.attachments.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaType
import com.cbgm.sparrow.feature.attachments.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.attachments.presentation.model.MediaSelectionSource
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSLock
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberGalleryPickerLauncher(
    maxItems: Int,
    selectedMedia: List<MediaSelection>,
    onMediaSelected: (List<MediaSelection>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): GalleryPickerLauncher {
    require(maxItems in 1..MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE)

    val viewController = LocalUIViewController.current
    val currentOnMediaSelected = rememberUpdatedState(onMediaSelected)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    val currentOnError = rememberUpdatedState(onError)
    val externalSelections = selectedMedia.filter { it.source != MediaSelectionSource.GALLERY }
    val gallerySelectionLimit = (maxItems - externalSelections.size).coerceAtLeast(1)
    val delegate =
        remember(gallerySelectionLimit, externalSelections) {
            GalleryPickerDelegate(
                maxItems = gallerySelectionLimit,
                onMediaSelected = { gallerySelections ->
                    currentOnMediaSelected.value(externalSelections + gallerySelections)
                },
                onDismissed = { currentOnDismissed.value() },
                onError = { currentOnError.value(it) }
            )
        }

    return remember(viewController, delegate, gallerySelectionLimit) {
        GalleryPickerLauncher(
            launch = {
                val configuration = PHPickerConfiguration()
                configuration.selectionLimit = gallerySelectionLimit.toLong()
                configuration.filter =
                    PHPickerFilter.anyFilterMatchingSubfilters(
                        listOf(PHPickerFilter.imagesFilter, PHPickerFilter.videosFilter)
                    )

                val picker = PHPickerViewController(configuration = configuration)
                picker.setDelegate(delegate)
                viewController.presentViewController(picker, animated = true, completion = null)
            }
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class GalleryPickerDelegate(
    private val maxItems: Int,
    private val onMediaSelected: (List<MediaSelection>) -> Unit,
    private val onDismissed: () -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val results = didFinishPicking.filterIsInstance<PHPickerResult>().take(maxItems)
        if (results.isEmpty()) {
            onDismissed()
            return
        }

        val collector = GallerySelectionCollector(results.size, onMediaSelected, onError)
        results.forEachIndexed { index, result ->
            val provider = result.itemProvider
            when {
                provider.hasItemConformingToTypeIdentifier(IMAGE_TYPE_IDENTIFIER) ->
                    provider.loadDataRepresentationForTypeIdentifier(IMAGE_TYPE_IDENTIFIER) { data, error ->
                        val selection =
                            if (data != null && error == null) {
                                runCatching { decodeGalleryImage(data) }.getOrNull()
                            } else {
                                null
                            }
                        collector.complete(
                            index = index,
                            selection = selection,
                            errorMessage = error?.localizedDescription ?: if (selection == null) "Selected image could not be read" else null
                        )
                    }

                provider.hasItemConformingToTypeIdentifier(VIDEO_TYPE_IDENTIFIER) ->
                    provider.loadDataRepresentationForTypeIdentifier(VIDEO_TYPE_IDENTIFIER) { data, error ->
                        val selection =
                            if (data != null && error == null) {
                                runCatching { decodeGalleryVideo(data) }.getOrNull()
                            } else {
                                null
                            }
                        collector.complete(
                            index = index,
                            selection = selection,
                            errorMessage = error?.localizedDescription ?: if (selection == null) "Selected video could not be read" else null
                        )
                    }

                else -> collector.complete(index, null, "Selected item is not a supported image or video")
            }
        }
    }
}

private class GallerySelectionCollector(
    count: Int,
    private val onMediaSelected: (List<MediaSelection>) -> Unit,
    private val onError: (String) -> Unit
) {
    private val lock = NSLock()
    private val results = arrayOfNulls<MediaSelection>(count)
    private var remaining = count
    private var firstError: String? = null

    fun complete(
        index: Int,
        selection: MediaSelection?,
        errorMessage: String?
    ) {
        val completed: List<MediaSelection>?
        val error: String?
        lock.lock()
        try {
            results[index] = selection
            if (firstError == null && errorMessage != null) firstError = errorMessage
            remaining -= 1
            if (remaining == 0) {
                completed = results.filterNotNull()
                error = firstError
            } else {
                completed = null
                error = null
            }
        } finally {
            lock.unlock()
        }

        if (completed == null) return
        if (completed.isEmpty()) {
            onError(error ?: "Selected gallery media could not be read")
        } else {
            onMediaSelected(completed)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun decodeGalleryImage(data: NSData): MediaSelection {
    val image = requireNotNull(UIImage(data = data)) { "Selected image could not be decoded" }
    val normalized = image.normalizedForMessageAttachment()
    val bytes = normalized.encodeWithinLimit()
    return MediaSelection(
        id = IdGenerator.generate(prefix = "gallery-image"),
        type = MessageMediaType.IMAGE,
        bytes = bytes,
        mimeType = "image/jpeg",
        width = normalized.size.width.roundToInt().coerceAtLeast(1),
        height = normalized.size.height.roundToInt().coerceAtLeast(1)
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun decodeGalleryVideo(data: NSData): MediaSelection {
    require(data.length <= MessageAttachmentPolicy.MAX_VIDEO_BYTES.toULong()) {
        "Selected video exceeds ${MessageAttachmentPolicy.MAX_VIDEO_BYTES} bytes"
    }
    return MediaSelection(
        id = IdGenerator.generate(prefix = "gallery-video"),
        type = MessageMediaType.VIDEO,
        bytes = data.toByteArray(),
        mimeType = "video/quicktime"
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.normalizedForMessageAttachment(): UIImage {
    val sourceWidth = size.width
    val sourceHeight = size.height
    require(sourceWidth > 0.0 && sourceHeight > 0.0) { "Selected image has invalid dimensions" }

    val longestSide = max(sourceWidth, sourceHeight)
    val scale =
        if (longestSide > MessageAttachmentPolicy.MAX_IMAGE_DIMENSION) {
            MessageAttachmentPolicy.MAX_IMAGE_DIMENSION.toDouble() / longestSide
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
    return try {
        drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        requireNotNull(UIGraphicsGetImageFromCurrentImageContext()) {
            "Selected image could not be normalized"
        }
    } finally {
        UIGraphicsEndImageContext()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.encodeWithinLimit(): ByteArray {
    for (quality in JPEG_QUALITIES) {
        val bytes = UIImageJPEGRepresentation(this, quality)?.toByteArray() ?: continue
        if (bytes.size <= MessageAttachmentPolicy.MAX_IMAGE_BYTES) return bytes
    }
    error("Selected image is too large after normalization")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    require(length <= Int.MAX_VALUE.toULong()) { "Attachment is too large" }
    if (length == 0UL) return ByteArray(0)
    return ByteArray(length.toInt()).also { result ->
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}

private const val IMAGE_TYPE_IDENTIFIER = "public.image"
private const val VIDEO_TYPE_IDENTIFIER = "public.movie"
private val JPEG_QUALITIES = listOf(0.90, 0.84, 0.78, 0.72, 0.66, 0.60)
