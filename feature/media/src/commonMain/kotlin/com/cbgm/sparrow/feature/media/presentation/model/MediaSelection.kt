package com.cbgm.sparrow.feature.media.presentation.model

enum class MediaSelectionSource { GALLERY, CAMERA, FILE_PICKER }

enum class MediaSelectionType { IMAGE, VIDEO, FILE }

/** No ByteArrays in composer state: pending files are stored privately until sent or removed. */
data class MediaSelection(
    val id: String,
    val type: MediaSelectionType,
    val localFilePath: String,
    val byteSize: Long,
    val mimeType: String,
    val source: MediaSelectionSource,
    val sourceReference: String? = null,
    val fileName: String? = null,
    val thumbnailFilePath: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
)
