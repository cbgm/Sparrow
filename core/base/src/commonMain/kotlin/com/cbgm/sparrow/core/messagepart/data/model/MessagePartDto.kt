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
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val fileName: String? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null
) : MessagePartDto

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
) : MessagePartDto

data class FileDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String,
    val localFilePath: String? = null
) : MessagePartDto

data class VoiceDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long,
    val durationMilliseconds: Long,
    val localFilePath: String? = null
) : MessagePartDto

data class LocationDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long
) : MessagePartDto

data class ContactDto(
    override val id: String,
    val blob: EncryptedBlobReferenceDto,
    val mimeType: String,
    val byteSize: Long
) : MessagePartDto

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
