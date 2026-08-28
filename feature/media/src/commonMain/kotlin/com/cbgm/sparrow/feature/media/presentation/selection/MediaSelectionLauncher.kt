package com.cbgm.sparrow.feature.media.presentation.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.cbgm.sparrow.feature.media.device.GalleryPickerStrings
import com.cbgm.sparrow.feature.media.device.rememberCameraCaptureLauncher
import com.cbgm.sparrow.feature.media.device.rememberGalleryPickerLauncher
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.GalleryPickerConfig
import com.cbgm.sparrow.feature.media.presentation.filepicker.FilePickerLauncher
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResult
import com.cbgm.sparrow.feature.media.presentation.mapper.toGalleryMedia
import com.cbgm.sparrow.feature.media.presentation.mapper.toMediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResult
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_media_choose_gallery
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

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
    maxFileBytes: Long,
    selectedMedia: List<MediaSelection>,
    onResult: (MediaSelectionResult) -> Unit,
    onFilePickerSessionStarted: (String) -> Unit,
    galleryTitle: String? = null,
    closeContentDescription: String? = null,
    filePickerLauncher: FilePickerLauncher = koinInject()
): MediaSelectionLauncher {
    val currentMedia by rememberUpdatedState(selectedMedia)
    val currentResult = rememberUpdatedState(onResult)
    val filePickerSessionStarted = rememberUpdatedState(onFilePickerSessionStarted)

    var activeFilePickerSessionId by remember { mutableStateOf<String?>(null) }

    val remainingCapacity = (maxItems - currentMedia.size).coerceAtLeast(0)
    val existingReferences = remember(currentMedia) {
        currentMedia.mapNotNullTo(mutableSetOf()) { it.sourceReference }
    }

    val tryAdd: (List<MediaSelection>) -> Unit = { additions ->
        if (remainingCapacity <= 0) {
            currentResult.value(MediaSelectionResult.Error(ERROR_LIMIT_REACHED))
        } else {
            currentResult.value(MediaSelectionResult.Selected(currentMedia + additions.take(remainingCapacity)))
        }
    }

    val galleryLauncher = rememberSubGalleryLauncher(
        maxItems = maxItems,
        remainingCapacity = remainingCapacity,
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes,
        galleryTitle = galleryTitle,
        closeContentDescription = closeContentDescription,
        currentMedia = currentMedia,
        currentResult = currentResult
    )

    val cameraLauncher = rememberSubCameraLauncher(
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes,
        tryAdd = tryAdd,
        currentResult = currentResult
    )

    ObserveFilePickerResults(
        filePickerLauncher = filePickerLauncher,
        activeSessionIdProvider = { activeFilePickerSessionId },
        onSessionReset = { activeFilePickerSessionId = null },
        existingReferences = existingReferences,
        tryAdd = tryAdd,
        currentResult = currentResult
    )

    return remember(galleryLauncher, cameraLauncher, filePickerLauncher, remainingCapacity, maxFileBytes, existingReferences) {
        object : MediaSelectionLauncher {
            override fun launch(source: MediaSelectionSource) {
                if (remainingCapacity <= 0) {
                    currentResult.value(MediaSelectionResult.Error(ERROR_LIMIT_REACHED))
                    return
                }

                when (source) {
                    MediaSelectionSource.GALLERY -> galleryLauncher.launch()
                    MediaSelectionSource.CAMERA -> cameraLauncher.launch()
                    MediaSelectionSource.FILE_PICKER -> {
                        val sessionId = filePickerLauncher.launch(
                            maxItems = remainingCapacity,
                            maxFileBytes = maxFileBytes,
                            blockedSourceReferences = existingReferences
                        )
                        activeFilePickerSessionId = sessionId
                        filePickerSessionStarted.value(sessionId)
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSubGalleryLauncher(
    maxItems: Int,
    remainingCapacity: Int,
    maxImageDimension: Int,
    maxImageBytes: Int,
    maxVideoBytes: Long,
    galleryTitle: String?,
    closeContentDescription: String?,
    currentMedia: List<MediaSelection>,
    currentResult: State<(MediaSelectionResult) -> Unit>
) = rememberGalleryPickerLauncher(
    config = GalleryPickerConfig(
        maxItems = remember(currentMedia, remainingCapacity) {
            (currentMedia.filter { it.source == MediaSelectionSource.GALLERY }.size + remainingCapacity).coerceAtLeast(1)
        },
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes
    ),
    selectedMedia = remember(currentMedia) {
        currentMedia.filter { it.source == MediaSelectionSource.GALLERY }.map(MediaSelection::toGalleryMedia)
    },
    strings = GalleryPickerStrings(
        title = galleryTitle ?: stringResource(Res.string.feature_media_choose_gallery),
        closeContentDescription = closeContentDescription ?: stringResource(Res.string.base_close)
    ),
    onMediaSelected = { picked ->
        val nonGallery = currentMedia.filter { it.source != MediaSelectionSource.GALLERY }
        val galleryIdsByReference = currentMedia
            .filter { it.source == MediaSelectionSource.GALLERY }
            .mapNotNull { it.sourceReference?.to(it.id) }.toMap()

        val mappedGallery = picked.map {
            it.toMediaSelection(existingId = it.sourceReference?.let(galleryIdsByReference::get))
        }
        currentResult.value(MediaSelectionResult.Selected((nonGallery + mappedGallery).take(maxItems)))
    },
    onDismissed = { currentResult.value(MediaSelectionResult.Dismissed) },
    onError = { msg -> currentResult.value(MediaSelectionResult.Error(msg)) }
)

@Composable
private fun rememberSubCameraLauncher(
    maxImageDimension: Int,
    maxImageBytes: Int,
    maxVideoBytes: Long,
    tryAdd: (List<MediaSelection>) -> Unit,
    currentResult: State<(MediaSelectionResult) -> Unit>
) = rememberCameraCaptureLauncher(
    config = CameraCaptureConfig(
        allowedTypes = setOf(CameraCaptureType.PHOTO, CameraCaptureType.VIDEO),
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes
    ),
    onCaptured = { captured -> tryAdd(listOf(captured.toMediaSelection())) },
    onDismissed = { currentResult.value(MediaSelectionResult.Dismissed) },
    onError = { msg -> currentResult.value(MediaSelectionResult.Error(msg)) }
)

@Composable
private fun ObserveFilePickerResults(
    filePickerLauncher: FilePickerLauncher,
    activeSessionIdProvider: () -> String?,
    onSessionReset: () -> Unit,
    existingReferences: Set<String?>,
    tryAdd: (List<MediaSelection>) -> Unit,
    currentResult: State<(MediaSelectionResult) -> Unit>
) {
    val activeSessionId = activeSessionIdProvider()

    LaunchedEffect(filePickerLauncher, activeSessionId) {
        filePickerLauncher.results.collect { pickerResult ->
            if (pickerResult.sessionId != activeSessionId) return@collect
            onSessionReset()

            when (pickerResult) {
                is FilePickerSessionResult.Completed -> {
                    val uniqueFiles = pickerResult.media.filterNot { it.sourceReference in existingReferences }
                    tryAdd(uniqueFiles)
                }
                is FilePickerSessionResult.Dismissed -> {
                    currentResult.value(MediaSelectionResult.Dismissed)
                }
                is FilePickerSessionResult.Failed -> {
                    currentResult.value(MediaSelectionResult.Error(pickerResult.message))
                }
            }
        }
    }
}
