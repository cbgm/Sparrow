package com.cbgm.sparrow.feature.media.presentation.mapper

import com.cbgm.sparrow.feature.media.domain.model.FileMediaSelection
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.domain.model.MediaSelection
import com.cbgm.sparrow.feature.media.domain.model.MediaSource
import com.cbgm.sparrow.feature.media.domain.model.VisualMediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.FileMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi

fun MediaSelection.toUi(): MediaSelectionUi =
    when (this) {
        is VisualMediaSelection ->
            VisualMediaSelectionUi(
                id = id,
                localFilePath = localFilePath,
                byteSize = byteSize,
                mimeType = mimeType,
                source = source.toUi(),
                sourceReference = sourceReference,
                type = type.toUi(),
                thumbnailFilePath = thumbnailFilePath,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds
            )

        is FileMediaSelection ->
            FileMediaSelectionUi(
                id = id,
                localFilePath = localFilePath,
                byteSize = byteSize,
                mimeType = mimeType,
                source = source.toUi(),
                sourceReference = sourceReference,
                fileName = fileName
            )
    }

fun MediaSelectionUi.toDomain(): MediaSelection =
    when (this) {
        is VisualMediaSelectionUi ->
            VisualMediaSelection(
                id = id,
                localFilePath = localFilePath,
                byteSize = byteSize,
                mimeType = mimeType,
                source = source.toDomain(),
                sourceReference = sourceReference,
                type = type.toDomain(),
                thumbnailFilePath = thumbnailFilePath,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds
            )

        is FileMediaSelectionUi ->
            FileMediaSelection(
                id = id,
                localFilePath = localFilePath,
                byteSize = byteSize,
                mimeType = mimeType,
                source = source.toDomain(),
                sourceReference = sourceReference,
                fileName = fileName
            )
    }

fun MediaContentType.toUi(): MediaTypeUi =
    when (this) {
        MediaContentType.IMAGE -> MediaTypeUi.IMAGE
        MediaContentType.VIDEO -> MediaTypeUi.VIDEO
    }

fun MediaTypeUi.toDomain(): MediaContentType =
    when (this) {
        MediaTypeUi.IMAGE -> MediaContentType.IMAGE
        MediaTypeUi.VIDEO -> MediaContentType.VIDEO
    }

private fun MediaSource.toUi(): MediaSourceUi =
    when (this) {
        MediaSource.GALLERY -> MediaSourceUi.GALLERY
        MediaSource.CAMERA -> MediaSourceUi.CAMERA
        MediaSource.FILE_PICKER -> MediaSourceUi.FILE_PICKER
    }

private fun MediaSourceUi.toDomain(): MediaSource =
    when (this) {
        MediaSourceUi.GALLERY -> MediaSource.GALLERY
        MediaSourceUi.CAMERA -> MediaSource.CAMERA
        MediaSourceUi.FILE_PICKER -> MediaSource.FILE_PICKER
    }
