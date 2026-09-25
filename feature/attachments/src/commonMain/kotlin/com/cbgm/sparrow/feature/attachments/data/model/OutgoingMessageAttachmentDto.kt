package com.cbgm.sparrow.feature.attachments.data.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType

/** Data-layer transfer input. Validation of domain policy happens before mapping in the repository. */
data class OutgoingMessageAttachmentDto(
    val id: String,
    val type: MessageAttachmentType,
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String?,
    val width: Int?,
    val height: Int?,
    val durationMilliseconds: Long?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OutgoingMessageAttachmentDto) return false
        if (id != other.id) return false
        if (type != other.type) return false
        if (mimeType != other.mimeType) return false
        if (fileName != other.fileName) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (!bytes.contentEquals(other.bytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (width?.hashCode() ?: 0)
        result = 31 * result + (height?.hashCode() ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
