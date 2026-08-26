package com.cbgm.sparrow.feature.media.presentation.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cbgm.sparrow.feature.media.device.CameraCaptureLauncher
import com.cbgm.sparrow.feature.media.device.GalleryPickerLauncher
import com.cbgm.sparrow.feature.media.device.GalleryPickerStrings
import com.cbgm.sparrow.feature.media.device.rememberCameraCaptureLauncher
import com.cbgm.sparrow.feature.media.device.rememberGalleryPickerLauncher
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureConfig
import com.cbgm.sparrow.feature.media.domain.model.GalleryPickerConfig
import com.cbgm.sparrow.feature.media.presentation.filepicker.FilePickerLauncher
import com.cbgm.sparrow.feature.media.presentation.filepicker.rememberFilePickerLauncher
import com.cbgm.sparrow.feature.media.presentation.mapper.toGalleryMedia
import com.cbgm.sparrow.feature.media.presentation.mapper.toMediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.FileSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_media_choose_gallery
import org.jetbrains.compose.resources.stringResource

@Composable
fun rememberMediaSelectionCameraLauncher(
    config: CameraCaptureConfig,
    maxItems: Int,
    selectedMedia: List<MediaSelection>,
    onMediaSelected: (List<MediaSelection>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): CameraCaptureLauncher {
    val currentSelectedMedia = rememberUpdatedState(selectedMedia)
    val currentOnMediaSelected = rememberUpdatedState(onMediaSelected)
    val currentOnError = rememberUpdatedState(onError)

    return rememberCameraCaptureLauncher(
        config = config,
        onCaptured = { captured ->
            val current = currentSelectedMedia.value
            if (current.size >= maxItems) {
                currentOnError.value("No more media can be selected")
            } else {
                currentOnMediaSelected.value(current + captured.toMediaSelection())
            }
        },
        onDismissed = onDismissed,
        onError = onError
    )
}

@Composable
fun rememberMediaSelectionGalleryPickerLauncher(
    maxItems: Int,
    maxImageDimension: Int? = null,
    maxImageBytes: Int? = null,
    maxVideoBytes: Long? = null,
    selectedMedia: List<MediaSelection>,
    title: String? = null,
    closeContentDescription: String? = null,
    onMediaSelected: (List<MediaSelection>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): GalleryPickerLauncher {
    val resolvedTitle = title ?: stringResource(Res.string.feature_media_choose_gallery)
    val resolvedCloseContentDescription =
        closeContentDescription ?: stringResource(Res.string.base_close)
    val currentOnError = rememberUpdatedState(onError)
    val externalSelections = selectedMedia.filter { it.source != MediaSelectionSource.GALLERY }
    val availableGallerySlots = (maxItems - externalSelections.size).coerceAtLeast(0)

    if (availableGallerySlots == 0) {
        return remember(currentOnError) {
            GalleryPickerLauncher(
                launch = { currentOnError.value("No more media can be selected") }
            )
        }
    }

    val gallerySelections = selectedMedia.filter { it.source == MediaSelectionSource.GALLERY }
    val existingBySource = gallerySelections
        .mapNotNull { selection -> selection.sourceReference?.let { source -> source to selection.id } }
        .toMap()

    return rememberGalleryPickerLauncher(
        config = GalleryPickerConfig(
            maxItems = availableGallerySlots,
            maxImageDimension = maxImageDimension,
            maxImageBytes = maxImageBytes,
            maxVideoBytes = maxVideoBytes
        ),
        selectedMedia = gallerySelections.map(MediaSelection::toGalleryMedia),
        strings = GalleryPickerStrings(
            title = resolvedTitle,
            closeContentDescription = resolvedCloseContentDescription
        ),
        onMediaSelected = { picked ->
            onMediaSelected(
                externalSelections +
                    picked.map { media ->
                        media.toMediaSelection(
                            existingId = media.sourceReference?.let(existingBySource::get)
                        )
                    }
            )
        },
        onDismissed = onDismissed,
        onError = onError
    )
}

@Composable
fun rememberFileSelectionPickerLauncher(
    maxItems: Int,
    maxFileBytes: Long,
    selectedFiles: List<FileSelection>,
    onFilesSelected: (List<FileSelection>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit
): FilePickerLauncher {
    val currentSelectedFiles = rememberUpdatedState(selectedFiles)
    val remainingCapacity = (maxItems - selectedFiles.size).coerceAtLeast(0)

    return rememberFilePickerLauncher(
        maxItems = remainingCapacity,
        maxFileBytes = maxFileBytes,
        blockedSourceReferences = selectedFiles.mapNotNullTo(mutableSetOf()) { it.sourceReference },
        onFilesSelected = { pickedFiles ->
            val current = currentSelectedFiles.value
            val additions =
                pickedFiles.filterNot { picked ->
                    current.any { existing -> existing.sourceReference == picked.sourceReference }
                }
            onFilesSelected(current + additions)
        },
        onDismissed = onDismissed,
        onError = onError
    )
}
