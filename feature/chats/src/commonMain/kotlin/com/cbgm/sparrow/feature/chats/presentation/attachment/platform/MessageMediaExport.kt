package com.cbgm.sparrow.feature.chats.presentation.attachment.platform

import androidx.compose.runtime.Composable
import com.cbgm.sparrow.feature.chats.domain.model.attachment.MessageMediaType

data class MessageMediaExportItem(
    val attachmentId: String,
    val type: MessageMediaType,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageMediaExportItem

        if (attachmentId != other.attachmentId) return false
        if (type != other.type) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attachmentId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

interface MessageMediaExporter {
    suspend fun saveToCameraRoll(media: List<MessageMediaExportItem>): Result<Int>
}

@Composable
expect fun rememberMessageMediaExporter(): MessageMediaExporter
