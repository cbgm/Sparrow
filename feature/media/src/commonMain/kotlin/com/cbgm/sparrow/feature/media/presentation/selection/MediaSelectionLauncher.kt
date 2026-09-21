package com.cbgm.sparrow.feature.media.presentation.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.cbgm.sparrow.feature.media.device.GalleryPickerStrings
import com.cbgm.sparrow.feature.media.device.rememberCameraCaptureLauncher
import com.cbgm.sparrow.feature.media.device.rememberGalleryPickerLauncher
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.GalleryPickerConfig
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.filepicker.FilePickerLauncher
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResult
import com.cbgm.sparrow.feature.media.presentation.mapper.toMediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResult
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_media_choose_gallery
import kotlinx.coroutines.launch
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
    filePickerLauncher: FilePickerLauncher = koinInject(),
    mediaFiles: MediaSelectionFileRepository = koinInject()
): MediaSelectionLauncher {
    val scope = rememberCoroutineScope()
    val currentMedia by rememberUpdatedState(selectedMedia)
    val currentResult = rememberUpdatedState(onResult)
    val filePickerSessionStarted = rememberUpdatedState(onFilePickerSessionStarted)

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
        currentResult = currentResult,
        mediaFiles = mediaFiles,
        scope = scope
    )

    val cameraLauncher = rememberSubCameraLauncher(
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes,
        tryAdd = tryAdd,
        currentResult = currentResult,
        mediaFiles = mediaFiles,
        scope = scope
    )

    ObserveStateFilePickerResults(
        filePickerLauncher = filePickerLauncher,
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
    currentResult: State<(MediaSelectionResult) -> Unit>,
    mediaFiles: MediaSelectionFileRepository,
    scope: kotlinx.coroutines.CoroutineScope
) = rememberGalleryPickerLauncher(
    config = GalleryPickerConfig(
        maxItems = remember(currentMedia, remainingCapacity) {
            (currentMedia.filter { it.source == MediaSelectionSource.GALLERY }.size + remainingCapacity).coerceAtLeast(1)
        },
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes
    ),
    selectedSourceReferences = currentMedia
        .filter { it.source == MediaSelectionSource.GALLERY }
        .mapNotNull(MediaSelection::sourceReference),
    strings = GalleryPickerStrings(
        title = galleryTitle ?: stringResource(Res.string.feature_media_choose_gallery),
        closeContentDescription = closeContentDescription ?: stringResource(Res.string.base_close)
    ),
    onMediaSelected = { picked ->
        scope.launch {
            runCatching {
                val latest = currentMedia
                val nonGallery = latest.filter { it.source != MediaSelectionSource.GALLERY }
                val previousByReference = latest.filter { it.source == MediaSelectionSource.GALLERY }
                    .mapNotNull { selection -> selection.sourceReference?.let { it to selection } }.toMap()
                val mapped = mutableListOf<MediaSelection>()
                try {
                    picked.forEach { item ->
                        mapped += item.toMediaSelection(mediaFiles, item.sourceReference?.let(previousByReference::get))
                    }
                } catch (error: Exception) {
                    // Reused selections are still owned by the composer and must not be deleted.
                    val previousIds = latest.mapTo(mutableSetOf(), MediaSelection::id)
                    mapped.filterNot { it.id in previousIds }.forEach { selection ->
                        runCatching { mediaFiles.delete(selection.localFilePath) }
                        selection.thumbnailFilePath?.let { path -> runCatching { mediaFiles.delete(path) } }
                    }
                    throw error
                }
                currentResult.value(MediaSelectionResult.Selected((nonGallery + mapped).take(maxItems)))
            }.onFailure { error -> currentResult.value(MediaSelectionResult.Error(error.message ?: "Selected media could not be stored")) }
        }
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
    currentResult: State<(MediaSelectionResult) -> Unit>,
    mediaFiles: MediaSelectionFileRepository,
    scope: kotlinx.coroutines.CoroutineScope
) = rememberCameraCaptureLauncher(
    config = CameraCaptureConfig(
        allowedTypes = setOf(CameraCaptureType.PHOTO, CameraCaptureType.VIDEO),
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes
    ),
    onCaptured = { captured ->
        scope.launch {
            runCatching { captured.toMediaSelection(mediaFiles) }
                .onSuccess { tryAdd(listOf(it)) }
                .onFailure { currentResult.value(MediaSelectionResult.Error(it.message ?: "Camera media could not be stored")) }
        }
    },
    onDismissed = { currentResult.value(MediaSelectionResult.Dismissed) },
    onError = { msg -> currentResult.value(MediaSelectionResult.Error(msg)) }
)

@Composable
private fun ObserveStateFilePickerResults(
    filePickerLauncher: FilePickerLauncher,
    existingReferences: Set<String?>,
    tryAdd: (List<MediaSelection>) -> Unit,
    currentResult: State<(MediaSelectionResult) -> Unit>
) {
    val filePickerResults by filePickerLauncher.results.collectAsState()

    LaunchedEffect(filePickerResults) {
        val pickerResult = filePickerLauncher.consumeResult() ?: return@LaunchedEffect

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
