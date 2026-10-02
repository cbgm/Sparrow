package com.cbgm.sparrow.feature.chats.data.model

sealed interface MessagePartDto {
    data class TextDto(
        val text: String
    ) : MessagePartDto

    data class ImageVideoDto(
        val id: String,
        val type: ImageVideoTypeDto,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null,
        val localFilePath: String? = null
    ) : MessagePartDto

    data class FileDto(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String,
        val localFilePath: String? = null
    ) : MessagePartDto

    data class LocationDto(
        val id: String
    ) : MessagePartDto

    data class ContactDto(
        val id: String
    ) : MessagePartDto

    data class VoiceDto(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val durationMilliseconds: Long
    ) : MessagePartDto

    data class PollDto(
        val id: String,
        val question: String,
        val description: String? = null,
        val options: List<PollOptionDto> = emptyList(),
        val allowMultipleSelection: Boolean = false,
        val allowVoteChange: Boolean = true,
        val isAnonymous: Boolean = false,
        val expiresAtEpochMilliseconds: Long? = null
    ) : MessagePartDto
}

data class PollOptionDto(
    val id: String,
    val text: String
)

enum class ImageVideoTypeDto {
    IMAGE,
    VIDEO
}
