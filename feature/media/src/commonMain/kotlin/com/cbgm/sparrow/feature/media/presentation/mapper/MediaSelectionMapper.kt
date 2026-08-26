package com.cbgm.sparrow.feature.media.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CapturedMedia
import com.cbgm.sparrow.feature.media.domain.model.GalleryMedia
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
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
            CameraCaptureType.PHOTO -> MediaType.IMAGE
            CameraCaptureType.VIDEO -> MediaType.VIDEO
        },
        bytes = bytes,
        mimeType = mimeType,
        source = MediaSelectionSource.CAMERA,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )

fun MediaSelection.toGalleryMedia(): GalleryMedia =
    GalleryMedia(
        type = when (type) {
            MediaType.IMAGE -> MediaContentType.IMAGE
            MediaType.VIDEO -> MediaContentType.VIDEO
        },
        bytes = bytes,
        mimeType = mimeType,
        sourceReference = sourceReference,
        previewBytes = previewBytes,
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
            MediaContentType.IMAGE -> MediaType.IMAGE
            MediaContentType.VIDEO -> MediaType.VIDEO
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

fun MediaSelection.toMediaItem(): MediaItem =
    MediaItem(
        id = id,
        type = type,
        mimeType = mimeType,
        bytes = bytes,
        thumbnailBytes = previewBytes,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
