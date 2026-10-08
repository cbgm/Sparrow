package com.cbgm.sparrow.feature.media.presentation.model

enum class MediaSourceUi { GALLERY, CAMERA, FILE_PICKER }

/** No ByteArrays in composer state: pending files are stored privately until sent or removed. */
sealed interface MediaSelectionUi {
    val id: String
    val localFilePath: String
    val byteSize: Long
    val mimeType: String
    val source: MediaSourceUi
    val sourceReference: String?
}

data class VisualMediaSelectionUi(
    override val id: String,
    override val localFilePath: String,
    override val byteSize: Long,
    override val mimeType: String,
    override val source: MediaSourceUi,
    override val sourceReference: String? = null,
    override val type: MediaTypeUi,
    override val thumbnailFilePath: String? = null,
    override val width: Int? = null,
    override val height: Int? = null,
    override val durationMilliseconds: Long? = null
) : MediaSelectionUi,
    VisualMediaUi

data class FileMediaSelectionUi(
    override val id: String,
    override val localFilePath: String,
    override val byteSize: Long,
    override val mimeType: String,
    override val source: MediaSourceUi = MediaSourceUi.FILE_PICKER,
    override val sourceReference: String? = null,
    val fileName: String
) : MediaSelectionUi

val MediaSelectionUi.localFilePaths: List<String>
    get() =
        when (this) {
            is VisualMediaSelectionUi -> listOfNotNull(localFilePath, thumbnailFilePath).distinct()
            is FileMediaSelectionUi -> listOf(localFilePath)
        }
