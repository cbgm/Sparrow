package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.core.protocol.attachment.LOCATION_MIME_TYPE
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType

data class OutgoingMessageAttachment(
    val id: String,
    val type: MessageAttachmentType,
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
) {
    init {
        require(id.isNotBlank()) { "Attachment ID must not be blank" }
        require(bytes.isNotEmpty()) { "Attachment must not be empty" }
        require(mimeType.isNotBlank()) { "Attachment MIME type must not be blank" }
        require(width == null || width > 0) { "Attachment width must be positive" }
        require(height == null || height > 0) { "Attachment height must be positive" }
        require(durationMilliseconds == null || durationMilliseconds >= 0L) {
            "Attachment duration must not be negative"
        }

        when (type) {
            MessageAttachmentType.IMAGE -> {
                require(mimeType.startsWith("image/")) { "Image attachment must use an image MIME type" }
                require(width != null && height != null) { "Image attachment requires dimensions" }
                require(fileName == null) { "Image attachment must not have a file name" }
                require(bytes.size <= MessageAttachmentPolicy.MAX_IMAGE_BYTES) {
                    "Image attachment exceeds ${MessageAttachmentPolicy.MAX_IMAGE_BYTES} bytes"
                }
            }

            MessageAttachmentType.VIDEO -> {
                require(mimeType.startsWith("video/")) { "Video attachment must use a video MIME type" }
                require(fileName == null) { "Video attachment must not have a file name" }
                require(bytes.size.toLong() <= MessageAttachmentPolicy.MAX_VIDEO_BYTES) {
                    "Video attachment exceeds ${MessageAttachmentPolicy.MAX_VIDEO_BYTES} bytes"
                }
            }

            MessageAttachmentType.FILE -> {
                require(!fileName.isNullOrBlank()) { "File attachment requires a file name" }
                require(width == null && height == null) { "File attachment must not have media dimensions" }
                require(durationMilliseconds == null) { "File attachment must not have a media duration" }
                require(bytes.size.toLong() <= MessageAttachmentPolicy.MAX_FILE_BYTES) {
                    "File attachment exceeds ${MessageAttachmentPolicy.MAX_FILE_BYTES} bytes"
                }
            }

            MessageAttachmentType.LOCATION -> {
                require(mimeType == LOCATION_MIME_TYPE) {
                    "Location attachment must use the Sparrow location MIME type"
                }
                require(fileName == null) { "Location attachment must not have a file name" }
                require(width == null && height == null) { "Location attachment must not have media dimensions" }
                require(durationMilliseconds == null) { "Location attachment must not have a media duration" }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OutgoingMessageAttachment) return false
        return id == other.id &&
            type == other.type &&
            bytes.contentEquals(other.bytes) &&
            mimeType == other.mimeType &&
            fileName == other.fileName &&
            width == other.width &&
            height == other.height &&
            durationMilliseconds == other.durationMilliseconds
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (width ?: 0)
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        return result
    }
}
