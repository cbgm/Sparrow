package com.cbgm.sparrow.feature.attachments.data.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment

data class PreparedMessageAttachmentDto(
    val attachment: MessageAttachment,
    val deleteCapability: String,
    val localFileName: String?,
    val payloadBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PreparedMessageAttachmentDto

        if (attachment != other.attachment) return false
        if (deleteCapability != other.deleteCapability) return false
        if (localFileName != other.localFileName) return false
        if (!payloadBytes.contentEquals(other.payloadBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attachment.hashCode()
        result = 31 * result + deleteCapability.hashCode()
        result = 31 * result + (localFileName?.hashCode() ?: 0)
        result = 31 * result + (payloadBytes?.contentHashCode() ?: 0)
        return result
    }
}
