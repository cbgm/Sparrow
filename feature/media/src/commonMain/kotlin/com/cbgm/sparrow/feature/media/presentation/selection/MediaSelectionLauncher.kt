package com.cbgm.sparrow.feature.media.presentation.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cbgm.sparrow.feature.media.device.GalleryPickerStrings
import com.cbgm.sparrow.feature.media.device.rememberCameraCaptureLauncher
import com.cbgm.sparrow.feature.media.device.rememberGalleryPickerLauncher
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.GalleryPickerConfig
import com.cbgm.sparrow.feature.media.presentation.mapper.toGalleryMedia
import com.cbgm.sparrow.feature.media.presentation.mapper.toMediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResult
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_media_choose_gallery
import org.jetbrains.compose.resources.stringResource

private const val ERROR_LIMIT_REACHED = "No more items can be selected"

interface MediaSelectionLauncher {
    fun launch(source: MediaSelectionSource)
}

@Composable
fun rememberMediaSelectionLauncher(
    maxItems: Int,
    maxImageDimension: Int,
    maxImageBytes: Int,
    maxVideoBytes: Long,
    selectedMedia: List<MediaSelection>,
    onResult: (MediaSelectionResult) -> Unit,
    galleryTitle: String? = null,
    closeContentDescription: String? = null
): MediaSelectionLauncher {
    val media = rememberUpdatedState(selectedMedia)
    val result = rememberUpdatedState(onResult)

    fun tryAdd(additions: List<MediaSelection>) {
        val current = media.value
        if (current.size >= maxItems) {
            result.value(MediaSelectionResult.Error(ERROR_LIMIT_REACHED))
        } else {
            result.value(MediaSelectionResult.Selected(current + additions.take(maxItems - current.size)))
        }
    }

    val gallerySelections =
        selectedMedia.filter { it.source == MediaSelectionSource.GALLERY }
    val galleryCapacity =
        (maxItems - (selectedMedia.size - gallerySelections.size)).coerceAtLeast(0)
    val galleryIdsByReference =
        gallerySelections.mapNotNull { it.sourceReference?.to(it.id) }.toMap()

    val galleryLauncher = rememberGalleryPickerLauncher(
        config = GalleryPickerConfig(
            maxItems = galleryCapacity.coerceAtLeast(1),
            maxImageDimension = maxImageDimension,
            maxImageBytes = maxImageBytes,
            maxVideoBytes = maxVideoBytes
        ),
        selectedMedia = gallerySelections.map(MediaSelection::toGalleryMedia),
        strings = GalleryPickerStrings(
            title = galleryTitle ?: stringResource(Res.string.feature_media_choose_gallery),
            closeContentDescription = closeContentDescription
                ?: stringResource(Res.string.base_close)
        ),
        onMediaSelected = { picked ->
            val nonGallery = media.value.filter { it.source != MediaSelectionSource.GALLERY }
            result.value(
                MediaSelectionResult.Selected(
                    nonGallery + picked.map {
                        it.toMediaSelection(
                            existingId = it.sourceReference?.let(galleryIdsByReference::get)
                        )
                    }
                )
            )
        },
        onDismissed = { result.value(MediaSelectionResult.Dismissed) },
        onError = { message -> result.value(MediaSelectionResult.Error(message)) }
    )

    val cameraLauncher = rememberCameraCaptureLauncher(
        config = CameraCaptureConfig(
            allowedTypes = setOf(CameraCaptureType.PHOTO, CameraCaptureType.VIDEO),
            maxImageDimension = maxImageDimension,
            maxImageBytes = maxImageBytes,
            maxVideoBytes = maxVideoBytes
        ),
        onCaptured = { captured -> tryAdd(listOf(captured.toMediaSelection())) },
        onDismissed = { result.value(MediaSelectionResult.Dismissed) },
        onError = { message -> result.value(MediaSelectionResult.Error(message)) }
    )

    return remember(galleryLauncher, cameraLauncher, galleryCapacity) {
        object : MediaSelectionLauncher {
            override fun launch(source: MediaSelectionSource) {
                when (source) {
                    MediaSelectionSource.GALLERY ->
                        if (galleryCapacity <= 0) {
                            result.value(MediaSelectionResult.Error(ERROR_LIMIT_REACHED))
                        } else {
                            galleryLauncher.launch()
                        }

                    MediaSelectionSource.CAMERA -> cameraLauncher.launch()
                    MediaSelectionSource.FILE_PICKER -> result.value(MediaSelectionResult.FilePickerRequested)
                }
            }
        }
    }
}
