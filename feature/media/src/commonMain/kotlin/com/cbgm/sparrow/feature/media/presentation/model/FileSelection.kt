package com.cbgm.sparrow.feature.media.presentation.model

data class FileSelection(
    val id: String,
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String,
    val sourceReference: String? = null
) {
    val byteSize: Long
        get() = bytes.size.toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileSelection) return false

        if (id != other.id) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false
        if (fileName != other.fileName) return false
        if (sourceReference != other.sourceReference) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + (sourceReference?.hashCode() ?: 0)
        return result
    }
}
