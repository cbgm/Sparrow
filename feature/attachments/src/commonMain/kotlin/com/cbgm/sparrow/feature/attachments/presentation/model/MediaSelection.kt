package com.cbgm.sparrow.feature.attachments.presentation.model

import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaType

enum class MediaSelectionSource {
    GALLERY,
    CAMERA
}

data class MediaSelection(
    val id: String,
    val type: MessageMediaType,
    val bytes: ByteArray,
    val mimeType: String,
    val source: MediaSelectionSource = MediaSelectionSource.GALLERY,
    val sourceReference: String? = null,
    val previewBytes: ByteArray? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaSelection

        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (id != other.id) return false
        if (type != other.type) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false
        if (source != other.source) return false
        if (sourceReference != other.sourceReference) return false
        if (!previewBytes.contentEquals(other.previewBytes)) return false

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
        result = 31 * result + source.hashCode()
        result = 31 * result + (sourceReference?.hashCode() ?: 0)
        result = 31 * result + (previewBytes?.contentHashCode() ?: 0)
        return result
    }
}
