package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentConstraints

enum class MessageMediaType {
    IMAGE,
    VIDEO
}

data class OutgoingMediaAttachment(
    val id: String,
    val type: MessageMediaType,
    val bytes: ByteArray,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
) {
    init {
        require(id.isNotBlank()) { "Attachment ID must not be blank" }
        require(bytes.isNotEmpty()) { "Attachment media must not be empty" }
        require(width == null || width > 0) { "Attachment width must be positive" }
        require(height == null || height > 0) { "Attachment height must be positive" }
        require(durationMilliseconds == null || durationMilliseconds >= 0L) {
            "Attachment duration must not be negative"
        }

        when (type) {
            MessageMediaType.IMAGE -> {
                require(mimeType.startsWith("image/")) { "Image attachment must use an image MIME type" }
                require(width != null && height != null) { "Image attachment requires dimensions" }
                require(bytes.size <= MessageAttachmentPolicy.MAX_IMAGE_BYTES) {
                    "Image attachment exceeds ${MessageAttachmentPolicy.MAX_IMAGE_BYTES} bytes"
                }
            }

            MessageMediaType.VIDEO -> {
                require(mimeType.startsWith("video/")) { "Video attachment must use a video MIME type" }
                require(bytes.size <= MessageAttachmentPolicy.MAX_VIDEO_BYTES) {
                    "Video attachment exceeds ${MessageAttachmentPolicy.MAX_VIDEO_BYTES} bytes"
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OutgoingMediaAttachment

        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (id != other.id) return false
        if (type != other.type) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width ?: 0
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

object MessageAttachmentPolicy {
    const val MAX_ATTACHMENTS_PER_MESSAGE = MessageAttachmentConstraints.MAX_ATTACHMENTS_PER_MESSAGE
    const val MAX_IMAGE_BYTES = 4 * 1024 * 1024
    const val MAX_VIDEO_BYTES = 64 * 1024 * 1024
    const val MAX_TOTAL_MEDIA_BYTES = 96L * 1024L * 1024L
    const val MAX_IMAGE_DIMENSION = 2048

    fun requireValid(media: List<OutgoingMediaAttachment>) {
        require(media.size <= MAX_ATTACHMENTS_PER_MESSAGE) {
            "A message can contain at most $MAX_ATTACHMENTS_PER_MESSAGE gallery attachments"
        }
        require(media.map(OutgoingMediaAttachment::id).distinct().size == media.size) {
            "Attachment IDs must be unique"
        }
        require(media.sumOf { it.bytes.size.toLong() } <= MAX_TOTAL_MEDIA_BYTES) {
            "Selected gallery media exceeds the total attachment size limit"
        }
    }
}
