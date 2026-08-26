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
import com.cbgm.sparrow.feature.media.presentation.filepicker.rememberFilePickerLauncher
import com.cbgm.sparrow.feature.media.presentation.mapper.toAttachmentSelection
import com.cbgm.sparrow.feature.media.presentation.mapper.toGalleryMedia
import com.cbgm.sparrow.feature.media.presentation.model.AttachmentSelection
import com.cbgm.sparrow.feature.media.presentation.model.AttachmentSelectionSource
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_close
import com.cbgm.sparrow.resources.feature_media_choose_gallery
import org.jetbrains.compose.resources.stringResource

private const val ERROR_LIMIT_REACHED = "No more attachments can be selected"

interface AttachmentSelectionLauncher {
    fun launch(source: AttachmentSelectionSource)
}

@Composable
fun rememberAttachmentSelectionLauncher(
    maxItems: Int,
    maxImageDimension: Int,
    maxImageBytes: Int,
    maxVideoBytes: Long,
    maxFileBytes: Long,
    selectedAttachments: List<AttachmentSelection>,
    onAttachmentsSelected: (List<AttachmentSelection>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit,
    galleryTitle: String? = null,
    closeContentDescription: String? = null
): AttachmentSelectionLauncher {
    val attachments = rememberUpdatedState(selectedAttachments)
    val select = rememberUpdatedState(onAttachmentsSelected)
    val error = rememberUpdatedState(onError)

    // Appends `additions` if there's room; otherwise reports the limit error.
    fun tryAdd(additions: List<AttachmentSelection>) {
        val current = attachments.value
        if (current.size >= maxItems) {
            error.value(ERROR_LIMIT_REACHED)
        } else {
            select.value(current + additions.take(maxItems - current.size))
        }
    }

    val gallerySelections =
        selectedAttachments.filter { it.source == AttachmentSelectionSource.GALLERY }
    val galleryCapacity =
        (maxItems - (selectedAttachments.size - gallerySelections.size)).coerceAtLeast(0)
    val galleryIdsByReference =
        gallerySelections.mapNotNull { it.sourceReference?.to(it.id) }.toMap()

    val galleryLauncher = rememberGalleryPickerLauncher(
        config = GalleryPickerConfig(
            maxItems = galleryCapacity.coerceAtLeast(1),
            maxImageDimension = maxImageDimension,
            maxImageBytes = maxImageBytes,
            maxVideoBytes = maxVideoBytes
        ),
        selectedMedia = gallerySelections.map(AttachmentSelection::toGalleryMedia),
        strings = GalleryPickerStrings(
            title = galleryTitle ?: stringResource(Res.string.feature_media_choose_gallery),
            closeContentDescription = closeContentDescription
                ?: stringResource(Res.string.base_close)
        ),
        onMediaSelected = { picked ->
            val nonGallery =
                attachments.value.filter { it.source != AttachmentSelectionSource.GALLERY }
            select.value(
                nonGallery + picked.map {
                    it.toAttachmentSelection(
                        existingId = it.sourceReference?.let(
                            galleryIdsByReference::get
                        )
                    )
                }
            )
        },
        onDismissed = onDismissed,
        onError = onError
    )

    val cameraLauncher = rememberCameraCaptureLauncher(
        config = CameraCaptureConfig(
            allowedTypes = setOf(CameraCaptureType.PHOTO, CameraCaptureType.VIDEO),
            maxImageDimension = maxImageDimension,
            maxImageBytes = maxImageBytes,
            maxVideoBytes = maxVideoBytes
        ),
        onCaptured = { captured -> tryAdd(listOf(captured.toAttachmentSelection())) },
        onDismissed = onDismissed,
        onError = onError
    )

    val fileLauncher = rememberFilePickerLauncher(
        maxItems = (maxItems - selectedAttachments.size).coerceAtLeast(0),
        maxFileBytes = maxFileBytes,
        blockedSourceReferences = selectedAttachments.mapNotNullTo(mutableSetOf()) { it.sourceReference },
        onFilesSelected = { picked ->
            val existingRefs = attachments.value.mapNotNullTo(mutableSetOf()) { it.sourceReference }
            tryAdd(picked.filterNot { it.sourceReference in existingRefs })
        },
        onDismissed = onDismissed,
        onError = onError
    )

    return remember(galleryLauncher, cameraLauncher, fileLauncher) {
        object : AttachmentSelectionLauncher {
            override fun launch(source: AttachmentSelectionSource) {
                when (source) {
                    AttachmentSelectionSource.GALLERY ->
                        if (galleryCapacity <= 0) error.value(ERROR_LIMIT_REACHED) else galleryLauncher.launch()

                    AttachmentSelectionSource.CAMERA -> cameraLauncher.launch()
                    AttachmentSelectionSource.FILE_PICKER -> fileLauncher.launch()
                }
            }
        }
    }
}
