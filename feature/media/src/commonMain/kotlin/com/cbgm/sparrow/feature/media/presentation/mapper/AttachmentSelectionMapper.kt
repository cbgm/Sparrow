package com.cbgm.sparrow.feature.media.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CapturedMedia
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.model.GalleryMedia
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.presentation.model.AttachmentSelection
import com.cbgm.sparrow.feature.media.presentation.model.AttachmentSelectionSource
import com.cbgm.sparrow.feature.media.presentation.model.AttachmentSelectionType
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

fun CapturedMedia.toAttachmentSelection(): AttachmentSelection =
    AttachmentSelection(
        id = IdGenerator.generate(
            prefix = when (type) {
                CameraCaptureType.PHOTO -> "camera-image"
                CameraCaptureType.VIDEO -> "camera-video"
            }
        ),
        type = when (type) {
            CameraCaptureType.PHOTO -> AttachmentSelectionType.IMAGE
            CameraCaptureType.VIDEO -> AttachmentSelectionType.VIDEO
        },
        bytes = bytes,
        mimeType = mimeType,
        source = AttachmentSelectionSource.CAMERA,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

fun GalleryMedia.toAttachmentSelection(existingId: String? = null): AttachmentSelection =
    AttachmentSelection(
        id = existingId ?: IdGenerator.generate(
            prefix = when (type) {
                MediaContentType.IMAGE -> "gallery-image"
                MediaContentType.VIDEO -> "gallery-video"
            }
        ),
        type = when (type) {
            MediaContentType.IMAGE -> AttachmentSelectionType.IMAGE
            MediaContentType.VIDEO -> AttachmentSelectionType.VIDEO
        },
        bytes = bytes,
        mimeType = mimeType,
        source = AttachmentSelectionSource.GALLERY,
        sourceReference = sourceReference,
        previewBytes = previewBytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

fun FileBrowserContent.toAttachmentSelection(): AttachmentSelection =
    AttachmentSelection(
        id = IdGenerator.generate(prefix = "file"),
        type = AttachmentSelectionType.FILE,
        bytes = bytes,
        mimeType = mimeType,
        source = AttachmentSelectionSource.FILE_PICKER,
        sourceReference = sourceReference,
        fileName = displayName
    )

fun AttachmentSelection.toGalleryMedia(): GalleryMedia {
    require(source == AttachmentSelectionSource.GALLERY) { "Only gallery selections can be restored in the gallery picker" }
    return GalleryMedia(
        type = when (type) {
            AttachmentSelectionType.IMAGE -> MediaContentType.IMAGE
            AttachmentSelectionType.VIDEO -> MediaContentType.VIDEO
            AttachmentSelectionType.FILE -> error("Files cannot be restored in the gallery picker")
        },
        bytes = bytes,
        mimeType = mimeType,
        sourceReference = sourceReference,
        previewBytes = previewBytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
}

fun AttachmentSelection.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type = when (type) {
            AttachmentSelectionType.IMAGE -> MediaType.IMAGE
            AttachmentSelectionType.VIDEO -> MediaType.VIDEO
            AttachmentSelectionType.FILE -> error("Files cannot be shown as media")
        },
        mimeType = mimeType,
        bytes = bytes,
        thumbnailBytes = previewBytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
