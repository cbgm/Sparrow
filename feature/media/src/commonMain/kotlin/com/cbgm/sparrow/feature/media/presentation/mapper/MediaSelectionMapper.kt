package com.cbgm.sparrow.feature.media.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CapturedMedia
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.model.GalleryMedia
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionType
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

fun CapturedMedia.toMediaSelection(): MediaSelection =
    MediaSelection(
        id = IdGenerator.generate(
            prefix = when (type) {
                CameraCaptureType.PHOTO -> "camera-image"
                CameraCaptureType.VIDEO -> "camera-video"
            }
        ),
        type = when (type) {
            CameraCaptureType.PHOTO -> MediaSelectionType.IMAGE
            CameraCaptureType.VIDEO -> MediaSelectionType.VIDEO
        },
        bytes = bytes,
        mimeType = mimeType,
        source = MediaSelectionSource.CAMERA,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

fun GalleryMedia.toMediaSelection(existingId: String? = null): MediaSelection =
    MediaSelection(
        id = existingId ?: IdGenerator.generate(
            prefix = when (type) {
                MediaContentType.IMAGE -> "gallery-image"
                MediaContentType.VIDEO -> "gallery-video"
            }
        ),
        type = when (type) {
            MediaContentType.IMAGE -> MediaSelectionType.IMAGE
            MediaContentType.VIDEO -> MediaSelectionType.VIDEO
        },
        bytes = bytes,
        mimeType = mimeType,
        source = MediaSelectionSource.GALLERY,
        sourceReference = sourceReference,
        previewBytes = previewBytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

fun FileBrowserContent.toMediaSelection(): MediaSelection =
    MediaSelection(
        id = IdGenerator.generate(prefix = "file"),
        type = MediaSelectionType.FILE,
        bytes = bytes,
        mimeType = mimeType,
        source = MediaSelectionSource.FILE_PICKER,
        sourceReference = sourceReference,
        fileName = displayName
    )

fun MediaSelection.toGalleryMedia(): GalleryMedia {
    require(source == MediaSelectionSource.GALLERY) { "Only gallery selections can be restored in the gallery picker" }
    return GalleryMedia(
        type = when (type) {
            MediaSelectionType.IMAGE -> MediaContentType.IMAGE
            MediaSelectionType.VIDEO -> MediaContentType.VIDEO
            MediaSelectionType.FILE -> error("Files cannot be restored in the gallery picker")
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

fun MediaSelection.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type = when (type) {
            MediaSelectionType.IMAGE -> MediaType.IMAGE
            MediaSelectionType.VIDEO -> MediaType.VIDEO
            MediaSelectionType.FILE -> error("Files cannot be shown as media")
        },
        mimeType = mimeType,
        bytes = bytes,
        thumbnailBytes = previewBytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
