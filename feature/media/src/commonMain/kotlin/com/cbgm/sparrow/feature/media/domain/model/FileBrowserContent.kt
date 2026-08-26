package com.cbgm.sparrow.feature.media.domain.model

data class FileBrowserContent(
    val sourceReference: String,
    val displayName: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileBrowserContent) return false
        return sourceReference == other.sourceReference &&
            displayName == other.displayName &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = sourceReference.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
