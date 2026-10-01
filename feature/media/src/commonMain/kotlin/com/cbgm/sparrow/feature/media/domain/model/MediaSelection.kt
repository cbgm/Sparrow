package com.cbgm.sparrow.feature.media.domain.model

enum class MediaSource { GALLERY, CAMERA, FILE_PICKER }

/** Pending media stored privately until it is sent or removed. */
sealed interface MediaSelection {
    val id: String
    val localFilePath: String
    val byteSize: Long
    val mimeType: String
    val source: MediaSource
    val sourceReference: String?
}

data class VisualMediaSelection(
    override val id: String,
    override val localFilePath: String,
    override val byteSize: Long,
    override val mimeType: String,
    override val source: MediaSource,
    override val sourceReference: String? = null,
    val type: MediaContentType,
    val thumbnailFilePath: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
) : MediaSelection

data class FileMediaSelection(
    override val id: String,
    override val localFilePath: String,
    override val byteSize: Long,
    override val mimeType: String,
    override val source: MediaSource = MediaSource.FILE_PICKER,
    override val sourceReference: String? = null,
    val fileName: String
) : MediaSelection
