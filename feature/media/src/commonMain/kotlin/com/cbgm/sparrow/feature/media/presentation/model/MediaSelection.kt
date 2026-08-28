package com.cbgm.sparrow.feature.media.presentation.model

enum class MediaSelectionSource {
    GALLERY,
    CAMERA,
    FILE_PICKER
}

enum class MediaSelectionType {
    IMAGE,
    VIDEO,
    FILE
}

data class MediaSelection(
    val id: String,
    val type: MediaSelectionType,
    val bytes: ByteArray,
    val mimeType: String,
    val source: MediaSelectionSource,
    val sourceReference: String? = null,
    val fileName: String? = null,
    val previewBytes: ByteArray? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
) {
    val byteSize: Long
        get() = bytes.size.toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaSelection) return false

        if (id != other.id) return false
        if (type != other.type) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false
        if (source != other.source) return false
        if (sourceReference != other.sourceReference) return false
        if (fileName != other.fileName) return false
        if (previewBytes == null && other.previewBytes != null) return false
        if (previewBytes != null && other.previewBytes == null) return false
        if (
            previewBytes != null &&
            other.previewBytes != null &&
            !previewBytes.contentEquals(other.previewBytes)
        ) {
            return false
        }
        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + (sourceReference?.hashCode() ?: 0)
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (previewBytes?.contentHashCode() ?: 0)
        result = 31 * result + (width ?: 0)
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        return result
    }
}
