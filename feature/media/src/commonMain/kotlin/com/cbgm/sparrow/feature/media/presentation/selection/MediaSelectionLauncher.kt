package com.cbgm.sparrow.feature.media.presentation.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.media.device.GalleryPickerStrings
import com.cbgm.sparrow.feature.media.device.rememberCameraCaptureLauncher
import com.cbgm.sparrow.feature.media.device.rememberGalleryPickerLauncher
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.GalleryPickerConfig
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.domain.usecase.PrepareMediaSelectionUseCase
import com.cbgm.sparrow.feature.media.presentation.filepicker.FilePickerLauncher
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResultUi
import com.cbgm.sparrow.feature.media.presentation.mapper.toDomain
import com.cbgm.sparrow.feature.media.presentation.mapper.toUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResultUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.model.localFilePaths
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_media_choose_gallery
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val ERROR_LIMIT_REACHED = "No more items can be selected"

interface MediaSelectionLauncher {
    fun launch(source: MediaSourceUi)
}

@Composable
fun rememberMediaSelectionLauncher(
    maxItems: Int,
    maxImageDimension: Int,
    maxImageBytes: Int,
    maxVideoBytes: Long,
    maxFileBytes: Long,
    selectedMedia: List<MediaSelectionUi>,
    onResult: (MediaSelectionResultUi) -> Unit,
    onFilePickerSessionStarted: (String) -> Unit,
    galleryTitle: String? = null,
    galleryImagesOnly: Boolean = false,
    closeContentDescription: String? = null,
    filePickerLauncher: FilePickerLauncher = koinInject(),
    mediaFiles: MediaSelectionFileRepository = koinInject(),
    prepareMediaSelection: PrepareMediaSelectionUseCase = koinInject()
): MediaSelectionLauncher {
    val scope = rememberCoroutineScope()
    val currentMedia by rememberUpdatedState(selectedMedia)
    val currentResult = rememberUpdatedState(onResult)
    val filePickerSessionStarted = rememberUpdatedState(onFilePickerSessionStarted)

    val remainingCapacity = (maxItems - currentMedia.size).coerceAtLeast(0)
    val existingReferences = remember(currentMedia) {
        currentMedia.mapNotNullTo(mutableSetOf()) { it.sourceReference }
    }

    val tryAdd: (List<MediaSelectionUi>) -> Unit = { additions ->
        if (remainingCapacity <= 0) {
            currentResult.value(MediaSelectionResultUi.Error(ERROR_LIMIT_REACHED))
        } else {
            currentResult.value(MediaSelectionResultUi.Selected(currentMedia + additions.take(remainingCapacity)))
        }
    }

    val galleryLauncher = rememberSubGalleryLauncher(
        maxItems = maxItems,
        remainingCapacity = remainingCapacity,
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes,
        galleryTitle = galleryTitle,
        imagesOnly = galleryImagesOnly,
        closeContentDescription = closeContentDescription,
        currentMedia = currentMedia,
        currentResult = currentResult,
        mediaFiles = mediaFiles,
        prepareMediaSelection = prepareMediaSelection,
        scope = scope
    )

    val cameraLauncher = rememberSubCameraLauncher(
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes,
        tryAdd = tryAdd,
        currentResult = currentResult,
        prepareMediaSelection = prepareMediaSelection,
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
            override fun launch(source: MediaSourceUi) {
                if (remainingCapacity <= 0) {
                    currentResult.value(MediaSelectionResultUi.Error(ERROR_LIMIT_REACHED))
                    return
                }

                when (source) {
                    MediaSourceUi.GALLERY -> galleryLauncher.launch()
                    MediaSourceUi.CAMERA -> cameraLauncher.launch()
                    MediaSourceUi.FILE_PICKER -> {
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
    imagesOnly: Boolean,
    closeContentDescription: String?,
    currentMedia: List<MediaSelectionUi>,
    currentResult: State<(MediaSelectionResultUi) -> Unit>,
    mediaFiles: MediaSelectionFileRepository,
    prepareMediaSelection: PrepareMediaSelectionUseCase,
    scope: kotlinx.coroutines.CoroutineScope
) = rememberGalleryPickerLauncher(
    config = GalleryPickerConfig(
        maxItems = remember(currentMedia, remainingCapacity) {
            (currentMedia.filter { it.source == MediaSourceUi.GALLERY }.size + remainingCapacity).coerceAtLeast(1)
        },
        maxImageDimension = maxImageDimension,
        maxImageBytes = maxImageBytes,
        maxVideoBytes = maxVideoBytes,
        imagesOnly = imagesOnly
    ),
    selectedSourceReferences = currentMedia
        .filter { it.source == MediaSourceUi.GALLERY }
        .mapNotNull(MediaSelectionUi::sourceReference),
    strings = GalleryPickerStrings(
        title = galleryTitle ?: stringResource(Res.string.feature_media_choose_gallery),
        closeContentDescription = closeContentDescription ?: stringResource(Res.string.base_close)
    ),
    onMediaSelected = { picked ->
        scope.launch {
            runCatching {
                val latest = currentMedia
                val nonGallery = latest.filter { it.source != MediaSourceUi.GALLERY }
                val previousByReference = latest.filter { it.source == MediaSourceUi.GALLERY }
                    .mapNotNull { selection -> selection.sourceReference?.let { it to selection } }.toMap()
                val mapped = mutableListOf<MediaSelectionUi>()
                try {
                    picked.forEach { item ->
                        mapped +=
                            prepareMediaSelection
                                .fromGalleryMedia(
                                    media = item,
                                    existing = item.sourceReference?.let(previousByReference::get)?.toDomain()
                                )
                                .toUi()
                    }
                } catch (error: Exception) {
                    // Reused selections are still owned by the composer and must not be deleted.
                    val previousIds = latest.mapTo(mutableSetOf(), MediaSelectionUi::id)
                    mapped.filterNot { it.id in previousIds }.forEach { selection ->
                        selection.localFilePaths.forEach { path ->
                            runCatching { mediaFiles.delete(path) }
                        }
                    }
                    throw error
                }
                currentResult.value(MediaSelectionResultUi.Selected((nonGallery + mapped).take(maxItems)))
            }.onFailure { error ->
                SparrowLog.error("MediaSelectionLauncher", "Selected media could not be stored", error)
                currentResult.value(MediaSelectionResultUi.Error(error.message ?: "Selected media could not be stored"))
            }
        }
    },
    onDismissed = { currentResult.value(MediaSelectionResultUi.Dismissed) },
    onError = { msg -> currentResult.value(MediaSelectionResultUi.Error(msg)) }
)

@Composable
private fun rememberSubCameraLauncher(
    maxImageDimension: Int,
    maxImageBytes: Int,
    maxVideoBytes: Long,
    tryAdd: (List<MediaSelectionUi>) -> Unit,
    currentResult: State<(MediaSelectionResultUi) -> Unit>,
    prepareMediaSelection: PrepareMediaSelectionUseCase,
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
            runCatching { prepareMediaSelection.fromCapturedMedia(captured).toUi() }
                .onSuccess { tryAdd(listOf(it)) }
                .onFailure { error ->
                    SparrowLog.error("MediaSelectionLauncher", "Camera media could not be stored", error)
                    currentResult.value(MediaSelectionResultUi.Error(error.message ?: "Camera media could not be stored"))
                }
        }
    },
    onDismissed = { currentResult.value(MediaSelectionResultUi.Dismissed) },
    onError = { msg -> currentResult.value(MediaSelectionResultUi.Error(msg)) }
)

@Composable
private fun ObserveStateFilePickerResults(
    filePickerLauncher: FilePickerLauncher,
    existingReferences: Set<String?>,
    tryAdd: (List<MediaSelectionUi>) -> Unit,
    currentResult: State<(MediaSelectionResultUi) -> Unit>
) {
    val filePickerResults by filePickerLauncher.results.collectAsState()

    LaunchedEffect(filePickerResults) {
        val pickerResult = filePickerLauncher.consumeResult() ?: return@LaunchedEffect

        when (pickerResult) {
            is FilePickerSessionResultUi.Completed -> {
                val uniqueFiles = pickerResult.media.filterNot { it.sourceReference in existingReferences }
                tryAdd(uniqueFiles)
            }
            is FilePickerSessionResultUi.Dismissed -> {
                currentResult.value(MediaSelectionResultUi.Dismissed)
            }
            is FilePickerSessionResultUi.Failed -> {
                currentResult.value(MediaSelectionResultUi.Error(pickerResult.message))
            }
        }
    }
}
