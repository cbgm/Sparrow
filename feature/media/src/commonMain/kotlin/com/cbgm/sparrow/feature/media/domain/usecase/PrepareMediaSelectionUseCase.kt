package com.cbgm.sparrow.feature.media.domain.usecase

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.media.domain.model.CameraCaptureType
import com.cbgm.sparrow.feature.media.domain.model.CapturedMedia
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.model.FileMediaSelection
import com.cbgm.sparrow.feature.media.domain.model.GalleryMedia
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.domain.model.MediaSelection
import com.cbgm.sparrow.feature.media.domain.model.MediaSource
import com.cbgm.sparrow.feature.media.domain.model.VisualMediaSelection
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository

class PrepareMediaSelectionUseCase(
    private val files: MediaSelectionFileRepository
) {
    suspend fun fromCapturedMedia(captured: CapturedMedia): MediaSelection {
        val type =
            when (captured.type) {
                CameraCaptureType.PHOTO -> MediaContentType.IMAGE
                CameraCaptureType.VIDEO -> MediaContentType.VIDEO
            }
        val id = IdGenerator.generate(prefix = if (type == MediaContentType.IMAGE) "camera-image" else "camera-video")
        val saved = files.save(id, captured.bytes, null)

        return VisualMediaSelection(
            id = id,
            localFilePath = saved.localFilePath,
            byteSize = captured.bytes.size.toLong(),
            mimeType = captured.mimeType,
            source = MediaSource.CAMERA,
            type = type,
            width = captured.width,
            height = captured.height,
            durationMilliseconds = captured.durationMilliseconds
        )
    }

    suspend fun fromGalleryMedia(
        media: GalleryMedia,
        existing: MediaSelection? = null
    ): MediaSelection {
        if (
            existing != null &&
            existing.sourceReference == media.sourceReference &&
            media.sourceReference != null
        ) {
            return existing
        }

        val id = IdGenerator.generate(prefix = if (media.type == MediaContentType.IMAGE) "gallery-image" else "gallery-video")
        val saved = files.save(id, media.bytes, media.previewBytes)

        return VisualMediaSelection(
            id = id,
            localFilePath = saved.localFilePath,
            thumbnailFilePath = saved.thumbnailFilePath,
            byteSize = media.bytes.size.toLong(),
            mimeType = media.mimeType,
            source = MediaSource.GALLERY,
            sourceReference = media.sourceReference,
            type = media.type,
            width = media.width,
            height = media.height,
            durationMilliseconds = media.durationMilliseconds
        )
    }

    suspend fun fromFileBrowserContent(content: FileBrowserContent): MediaSelection {
        val id = IdGenerator.generate(prefix = "file")
        val saved = files.save(id, content.bytes, null)
        return FileMediaSelection(
            id = id,
            localFilePath = saved.localFilePath,
            byteSize = content.bytes.size.toLong(),
            mimeType = content.mimeType,
            sourceReference = content.sourceReference,
            fileName = content.displayName
        )
    }
}
