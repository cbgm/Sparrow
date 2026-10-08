@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.cbgm.sparrow.core.messagepart.data.model

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator

const val LOCATION_MIME_TYPE = "application/vnd.sparrow.location"
const val CONTACT_MIME_TYPE = "application/vnd.sparrow.contact"

@Serializable
@JsonClassDiscriminator("partType")
sealed interface MessagePartDto {
    val id: String
}

@Serializable
@SerialName("TEXT")
data class TextDto(
    override val id: String,
    val text: String
) : MessagePartDto

@Serializable
@SerialName("IMAGE")
data class ImageDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val fileName: String? = null,
    @Transient
    val localFilePath: String? = null,
    @Transient
    val thumbnailFilePath: String? = null,
    @Transient
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

@Serializable
@SerialName("VIDEO")
data class VideoDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null,
    @Transient
    val localFilePath: String? = null,
    @Transient
    val thumbnailFilePath: String? = null,
    @Transient
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

@Serializable
@SerialName("FILE")
data class FileDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String,
    @Transient
    val localFilePath: String? = null,
    @Transient
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

@Serializable
@SerialName("VOICE")
data class VoiceDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String,
    val byteSize: Long,
    val durationMilliseconds: Long,
    @Transient
    val localFilePath: String? = null,
    @Transient
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

@Serializable
@SerialName("LOCATION")
data class LocationDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String = LOCATION_MIME_TYPE,
    val byteSize: Long,
    @Transient
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

@Serializable
@SerialName("CONTACT")
data class ContactDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto? = null,
    val mimeType: String = CONTACT_MIME_TYPE,
    val byteSize: Long,
    @Transient
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

@Serializable
@SerialName("POLL")
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

@Serializable
data class PollOptionDto(
    val id: String,
    val text: String,
    val voterIds: Set<String> = emptySet()
)
