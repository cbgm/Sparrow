package com.cbgm.sparrow.feature.media.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CapturedMedia
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.model.GalleryMedia
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.model.MediaItem
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionSource
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionType
import com.cbgm.sparrow.feature.media.presentation.model.MediaType

suspend fun CapturedMedia.toMediaSelection(files: MediaSelectionFileRepository): MediaSelection {
    val id = IdGenerator.generate(prefix = if (type == CameraCaptureType.PHOTO) "camera-image" else "camera-video")
    val saved = files.save(id, bytes, null)
    return MediaSelection(
        id = id,
        type = if (type == CameraCaptureType.PHOTO) MediaSelectionType.IMAGE else MediaSelectionType.VIDEO,
        localFilePath = saved.localFilePath,
        byteSize = bytes.size.toLong(),
        mimeType = mimeType,
        source = MediaSelectionSource.CAMERA,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
}

suspend fun GalleryMedia.toMediaSelection(
    files: MediaSelectionFileRepository,
    existing: MediaSelection? = null
): MediaSelection {
    if (existing != null && existing.sourceReference == sourceReference && sourceReference != null) return existing
    val id = IdGenerator.generate(prefix = if (type == MediaContentType.IMAGE) "gallery-image" else "gallery-video")
    val saved = files.save(id, bytes, previewBytes)
    return MediaSelection(
        id = id,
        type = if (type == MediaContentType.IMAGE) MediaSelectionType.IMAGE else MediaSelectionType.VIDEO,
        localFilePath = saved.localFilePath,
        thumbnailFilePath = saved.thumbnailFilePath,
        byteSize = bytes.size.toLong(),
        mimeType = mimeType,
        source = MediaSelectionSource.GALLERY,
        sourceReference = sourceReference,
        width = width,
        height = height,
        durationMilliseconds = durationMilliseconds
    )
}

suspend fun FileBrowserContent.toMediaSelection(files: MediaSelectionFileRepository): MediaSelection {
    val id = IdGenerator.generate(prefix = "file")
    val saved = files.save(id, bytes, null)
    return MediaSelection(
        id = id,
        type = MediaSelectionType.FILE,
        localFilePath = saved.localFilePath,
        byteSize = bytes.size.toLong(),
        mimeType = mimeType,
        source = MediaSelectionSource.FILE_PICKER,
        sourceReference = sourceReference,
        fileName = displayName
    )
}

fun MediaSelection.toMediaItem(): MediaItem = MediaItem(
    id = id,
    type = when (type) {
        MediaSelectionType.IMAGE -> MediaType.IMAGE
        MediaSelectionType.VIDEO -> MediaType.VIDEO
        MediaSelectionType.FILE -> error("Files cannot be shown as media")
    },
    mimeType = mimeType,
    localFilePath = localFilePath,
    thumbnailFilePath = thumbnailFilePath,
    width = width,
    height = height,
    durationMilliseconds = durationMilliseconds
)
