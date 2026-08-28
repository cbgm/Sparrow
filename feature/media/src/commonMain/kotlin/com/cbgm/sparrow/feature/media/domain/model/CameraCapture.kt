package com.cbgm.sparrow.feature.media.domain.model

enum class CameraCaptureType {
    PHOTO,
    VIDEO
}

enum class CameraLens {
    FRONT,
    BACK
}

data class CameraCaptureConfig(
    val allowedTypes: Set<CameraCaptureType>,
    val initialLens: CameraLens = CameraLens.BACK,
    val initialType: CameraCaptureType = CameraCaptureType.PHOTO,
    val maxImageDimension: Int? = null,
    val maxImageBytes: Int? = null,
    val maxVideoBytes: Long? = null
) {
    init {
        require(allowedTypes.isNotEmpty()) { "At least one camera capture type must be allowed" }
        require(initialType in allowedTypes) { "Initial camera capture type must be allowed" }
        maxImageDimension?.let { require(it > 0) { "Maximum image dimension must be positive" } }
        maxImageBytes?.let { require(it > 0) { "Maximum image bytes must be positive" } }
        maxVideoBytes?.let { require(it > 0L) { "Maximum video bytes must be positive" } }
    }
}

data class CapturedMedia(
    val type: CameraCaptureType,
    val bytes: ByteArray,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
) {
    init {
        require(bytes.isNotEmpty()) { "Captured media bytes must not be empty" }
        require(mimeType.isNotBlank()) { "Captured media MIME type must not be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CapturedMedia

        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (type != other.type) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width ?: 0
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}
