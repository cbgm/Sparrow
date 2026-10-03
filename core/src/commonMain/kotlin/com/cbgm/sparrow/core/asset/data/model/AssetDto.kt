package com.cbgm.sparrow.core.asset.data.model

sealed interface AssetDto {
    val id: String
}

data class TextDto(
    override val id: String,
    val text: String
) : AssetDto

data class ImageDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val fileName: String? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null
) : AssetDto

data class VideoDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null
) : AssetDto

data class FileDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String,
    val localFilePath: String? = null
) : AssetDto

data class VoiceDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long,
    val durationMilliseconds: Long
) : AssetDto

data class LocationDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long
) : AssetDto

data class ContactDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long
) : AssetDto

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
) : AssetDto

data class PollOptionDto(
    val id: String,
    val text: String
)

data class EncryptedBlobReferenceDto(
    val nodeId: String,
    val blobId: String,
    val readCapability: String,
    val ciphertextByteSize: Long,
    val expiresAtEpochMilliseconds: Long,
    val encryptionKey: ByteArray,
    val nonce: ByteArray,
    val ciphertextSha256: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedBlobReferenceDto) return false

        return nodeId == other.nodeId &&
            blobId == other.blobId &&
            readCapability == other.readCapability &&
            ciphertextByteSize == other.ciphertextByteSize &&
            expiresAtEpochMilliseconds == other.expiresAtEpochMilliseconds &&
            encryptionKey.contentEquals(other.encryptionKey) &&
            nonce.contentEquals(other.nonce) &&
            ciphertextSha256.contentEquals(other.ciphertextSha256)
    }

    override fun hashCode(): Int {
        var result = nodeId.hashCode()
        result = 31 * result + blobId.hashCode()
        result = 31 * result + readCapability.hashCode()
        result = 31 * result + ciphertextByteSize.hashCode()
        result = 31 * result + expiresAtEpochMilliseconds.hashCode()
        result = 31 * result + encryptionKey.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertextSha256.contentHashCode()
        return result
    }
}
