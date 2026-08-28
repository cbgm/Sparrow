package com.cbgm.sparrow.feature.media.presentation.model

enum class MediaType {
    IMAGE,
    VIDEO
}

/**
 * Decrypted media presented by Sparrow UI.
 *
 * A single photo is represented as a one-item list when opened in [MediaViewer].
 */
data class MediaItem(
    val id: String,
    val type: MediaType,
    val mimeType: String,
    val bytes: ByteArray? = null,
    val thumbnailBytes: ByteArray? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaItem

        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (id != other.id) return false
        if (type != other.type) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (!thumbnailBytes.contentEquals(other.thumbnailBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width ?: 0
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        result = 31 * result + (thumbnailBytes?.contentHashCode() ?: 0)
        return result
    }
}
