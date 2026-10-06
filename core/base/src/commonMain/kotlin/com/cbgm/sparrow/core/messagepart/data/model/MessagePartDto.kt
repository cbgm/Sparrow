package com.cbgm.sparrow.core.messagepart.data.model

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto

sealed interface MessagePartDto {
    val id: String
}

data class TextDto(
    override val id: String,
    val text: String
) : MessagePartDto

data class ImageDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val fileName: String? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null,
    val bytes: ByteArray? = null
) : MessagePartDto {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageDto

        if (byteSize != other.byteSize) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (id != other.id) return false
        if (blob != other.blob) return false
        if (mimeType != other.mimeType) return false
        if (fileName != other.fileName) return false
        if (localFilePath != other.localFilePath) return false
        if (thumbnailFilePath != other.thumbnailFilePath) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = byteSize.hashCode()
        result = 31 * result + (width ?: 0)
        result = 31 * result + (height ?: 0)
        result = 31 * result + id.hashCode()
        result = 31 * result + (blob?.hashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (localFilePath?.hashCode() ?: 0)
        result = 31 * result + (thumbnailFilePath?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

data class VideoDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null,
    val bytes: ByteArray? = null
) : MessagePartDto {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoDto

        if (byteSize != other.byteSize) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (id != other.id) return false
        if (blob != other.blob) return false
        if (mimeType != other.mimeType) return false
        if (fileName != other.fileName) return false
        if (localFilePath != other.localFilePath) return false
        if (thumbnailFilePath != other.thumbnailFilePath) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = byteSize.hashCode()
        result = 31 * result + (width ?: 0)
        result = 31 * result + (height ?: 0)
        result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        result = 31 * result + (blob?.hashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + (localFilePath?.hashCode() ?: 0)
        result = 31 * result + (thumbnailFilePath?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

data class FileDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String,
    val localFilePath: String? = null,
    val bytes: ByteArray? = null
) : MessagePartDto {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileDto

        if (byteSize != other.byteSize) return false
        if (id != other.id) return false
        if (blob != other.blob) return false
        if (mimeType != other.mimeType) return false
        if (fileName != other.fileName) return false
        if (localFilePath != other.localFilePath) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = byteSize.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (blob?.hashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + (localFilePath?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

data class VoiceDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val durationMilliseconds: Long,
    val localFilePath: String? = null,
    val bytes: ByteArray? = null
) : MessagePartDto {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceDto

        if (byteSize != other.byteSize) return false
        if (durationMilliseconds != other.durationMilliseconds) return false
        if (id != other.id) return false
        if (blob != other.blob) return false
        if (mimeType != other.mimeType) return false
        if (localFilePath != other.localFilePath) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = byteSize.hashCode()
        result = 31 * result + durationMilliseconds.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (blob?.hashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (localFilePath?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

data class LocationDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val bytes: ByteArray? = null
) : MessagePartDto {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationDto

        if (byteSize != other.byteSize) return false
        if (id != other.id) return false
        if (blob != other.blob) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = byteSize.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (blob?.hashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

data class ContactDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val bytes: ByteArray? = null
) : MessagePartDto {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContactDto

        if (byteSize != other.byteSize) return false
        if (id != other.id) return false
        if (blob != other.blob) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = byteSize.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (blob?.hashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

data class PollDto(
    override val id: String,
    val question: String,
    val description: String? = null,
    val options: List<PollOptionDto> = emptyList(),
    val images: List<ImageDto> = emptyList(),
    val allowMultipleSelection: Boolean = false,
    val allowVoteChange: Boolean = true,
    val isAnonymous: Boolean = false,
    val expiresAtEpochMilliseconds: Long? = null,
    val closedAtEpochMilliseconds: Long? = null
) : MessagePartDto

data class PollOptionDto(
    val id: String,
    val text: String
)
