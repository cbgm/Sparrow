package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import com.cbgm.sparrow.feature.media.domain.model.GalleryMedia
import com.cbgm.sparrow.feature.media.domain.model.GalleryPickerConfig
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
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
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberGalleryPickerLauncher(
    config: GalleryPickerConfig,
    selectedMedia: List<GalleryMedia>,
    strings: GalleryPickerStrings,
    onMediaSelected: (List<GalleryMedia>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): GalleryPickerLauncher {
    val viewController = LocalUIViewController.current
    val currentOnMediaSelected = rememberUpdatedState(onMediaSelected)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    val currentOnError = rememberUpdatedState(onError)
    val delegate =
        remember(config) {
            GalleryPickerDelegate(
                config = config,
                onMediaSelected = { currentOnMediaSelected.value(it) },
                onDismissed = { currentOnDismissed.value() },
                onError = { currentOnError.value(it) }
            )
        }

    return remember(viewController, delegate, config.maxItems) {
        GalleryPickerLauncher(
            launch = {
                val configuration = PHPickerConfiguration()
                configuration.selectionLimit = config.maxItems.toLong()
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
    private val config: GalleryPickerConfig,
    private val onMediaSelected: (List<GalleryMedia>) -> Unit,
    private val onDismissed: () -> Unit,
    private val onError: (String) -> Unit
) : NSObject(),
    PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val results = didFinishPicking.filterIsInstance<PHPickerResult>().take(config.maxItems)
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
                                runCatching { decodeGalleryImage(data, config) }.getOrNull()
                            } else {
                                null
                            }
                        collector.complete(
                            index = index,
                            selection = selection,
                            errorMessage =
                                error?.localizedDescription
                                    ?: if (selection == null) "Selected image could not be read" else null
                        )
                    }

                provider.hasItemConformingToTypeIdentifier(VIDEO_TYPE_IDENTIFIER) ->
                    provider.loadDataRepresentationForTypeIdentifier(VIDEO_TYPE_IDENTIFIER) { data, error ->
                        val selection =
                            if (data != null && error == null) {
                                runCatching { decodeGalleryVideo(data, config) }.getOrNull()
                            } else {
                                null
                            }
                        collector.complete(
                            index = index,
                            selection = selection,
                            errorMessage =
                                error?.localizedDescription
                                    ?: if (selection == null) "Selected video could not be read" else null
                        )
                    }

                else -> collector.complete(index, null, "Selected item is not a supported image or video")
            }
        }
    }
}

private class GallerySelectionCollector(
    count: Int,
    private val onMediaSelected: (List<GalleryMedia>) -> Unit,
    private val onError: (String) -> Unit
) {
    private val lock = NSLock()
    private val results = arrayOfNulls<GalleryMedia>(count)
    private var remaining = count
    private var firstError: String? = null

    fun complete(
        index: Int,
        selection: GalleryMedia?,
        errorMessage: String?
    ) {
        val completed: List<GalleryMedia>?
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
private fun decodeGalleryImage(
    data: NSData,
    config: GalleryPickerConfig
): GalleryMedia {
    val image = requireNotNull(UIImage(data = data)) { "Selected image could not be decoded" }
    val normalized = image.normalized(config.maxImageDimension)
    val bytes = normalized.encode(config.maxImageBytes)
    return GalleryMedia(
        type = MediaContentType.IMAGE,
        bytes = bytes,
        mimeType = "image/jpeg",
        width = normalized.size.width.roundToInt().coerceAtLeast(1),
        height = normalized.size.height.roundToInt().coerceAtLeast(1)
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun decodeGalleryVideo(
    data: NSData,
    config: GalleryPickerConfig
): GalleryMedia {
    config.maxVideoBytes?.let { maxBytes ->
        require(data.length <= maxBytes.toULong()) {
            "Selected video exceeds $maxBytes bytes"
        }
    }
    return GalleryMedia(
        type = MediaContentType.VIDEO,
        bytes = data.toByteArray(),
        mimeType = "video/quicktime"
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.normalized(maxImageDimension: Int?): UIImage {
    val sourceWidth = size.width
    val sourceHeight = size.height
    require(sourceWidth > 0.0 && sourceHeight > 0.0) { "Selected image has invalid dimensions" }

    if (maxImageDimension == null) return this

    val longestSide = max(sourceWidth, sourceHeight)
    if (longestSide <= maxImageDimension.toDouble()) return this

    val scale = maxImageDimension.toDouble() / longestSide
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
private fun UIImage.encode(maxImageBytes: Int?): ByteArray {
    for (quality in JPEG_QUALITIES) {
        val bytes = UIImageJPEGRepresentation(this, quality)?.toByteArray() ?: continue
        if (maxImageBytes == null || bytes.size <= maxImageBytes) return bytes
    }
    error("Selected image is too large after normalization")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.toByteArray(): ByteArray {
    require(length <= Int.MAX_VALUE.toULong()) { "Media is too large" }
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
