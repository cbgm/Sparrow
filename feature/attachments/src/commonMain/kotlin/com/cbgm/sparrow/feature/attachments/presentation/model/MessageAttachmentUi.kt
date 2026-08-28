package com.cbgm.sparrow.feature.attachments.presentation.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType

data class MessageAttachmentUi(
    val id: String,
    val type: MessageAttachmentType,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null,
    val localFilePath: String? = null,
    val bytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageAttachmentUi) return false

        if (id != other.id) return false
        if (type != other.type) return false
        if (mimeType != other.mimeType) return false
        if (byteSize != other.byteSize) return false
        if (fileName != other.fileName) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (localFilePath != other.localFilePath) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + byteSize.hashCode()
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (width ?: 0)
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        result = 31 * result + (localFilePath?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}
