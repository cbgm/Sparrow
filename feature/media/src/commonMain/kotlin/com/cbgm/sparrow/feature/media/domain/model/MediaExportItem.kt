package com.cbgm.sparrow.feature.media.domain.model

data class MediaExportItem(
    val id: String,
    val type: MediaContentType,
    val mimeType: String,
    val bytes: ByteArray
) {
    init {
        require(id.isNotBlank()) { "Media export ID must not be blank" }
        require(mimeType.isNotBlank()) { "Media export MIME type must not be blank" }
        require(bytes.isNotEmpty()) { "Media export bytes must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaExportItem

        if (id != other.id) return false
        if (type != other.type) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
