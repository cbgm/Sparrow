package com.cbgm.sparrow.feature.chats.presentation.component.model

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation

sealed interface MessagePartUi {
    data class ImageVideoUi(
        val id: String,
        val type: ImageVideoTypeUi,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null,
        val localFilePath: String? = null,
        val bytes: ByteArray? = null
    ) : MessagePartUi {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ImageVideoUi) return false

            return id == other.id &&
                type == other.type &&
                mimeType == other.mimeType &&
                byteSize == other.byteSize &&
                fileName == other.fileName &&
                width == other.width &&
                height == other.height &&
                durationMilliseconds == other.durationMilliseconds &&
                localFilePath == other.localFilePath &&
                bytes.contentEquals(other.bytes)
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

    data class FileUi(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String,
        val localFilePath: String? = null,
        val bytes: ByteArray? = null
    ) : MessagePartUi {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FileUi) return false

            return id == other.id &&
                mimeType == other.mimeType &&
                byteSize == other.byteSize &&
                fileName == other.fileName &&
                localFilePath == other.localFilePath &&
                bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + byteSize.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + (localFilePath?.hashCode() ?: 0)
            result = 31 * result + (bytes?.contentHashCode() ?: 0)
            return result
        }
    }

    data class LocationUi(
        val id: String,
        val location: CurrentLocation? = null
    ) : MessagePartUi

    data class TextUi(
        val text: String,
        val isContentFailed: Boolean
    ) : MessagePartUi
}

enum class ImageVideoTypeUi {
    IMAGE,
    VIDEO
}
