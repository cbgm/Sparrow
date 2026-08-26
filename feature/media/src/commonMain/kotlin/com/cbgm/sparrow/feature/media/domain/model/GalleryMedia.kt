package com.cbgm.sparrow.feature.media.domain.model

data class GalleryPickerConfig(
    val maxItems: Int,
    val maxImageDimension: Int? = null,
    val maxImageBytes: Int? = null,
    val maxVideoBytes: Long? = null
) {
    init {
        require(maxItems > 0) { "Maximum gallery item count must be positive" }
        maxImageDimension?.let { require(it > 0) { "Maximum image dimension must be positive" } }
        maxImageBytes?.let { require(it > 0) { "Maximum image byte size must be positive" } }
        maxVideoBytes?.let { require(it > 0L) { "Maximum video byte size must be positive" } }
    }
}

data class GalleryMedia(
    val type: MediaContentType,
    val bytes: ByteArray,
    val mimeType: String,
    val sourceReference: String? = null,
    val previewBytes: ByteArray? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
) {
    init {
        require(bytes.isNotEmpty()) { "Gallery media bytes must not be empty" }
        require(mimeType.isNotBlank()) { "Gallery media MIME type must not be blank" }
        require(width == null || width > 0) { "Gallery media width must be positive" }
        require(height == null || height > 0) { "Gallery media height must be positive" }
        require(durationMilliseconds == null || durationMilliseconds >= 0L) {
            "Gallery media duration must not be negative"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryMedia

        if (type != other.type) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false
        if (sourceReference != other.sourceReference) return false
        if (!previewBytes.contentEquals(other.previewBytes)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (sourceReference?.hashCode() ?: 0)
        result = 31 * result + (previewBytes?.contentHashCode() ?: 0)
        result = 31 * result + (width ?: 0)
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        return result
    }
}
