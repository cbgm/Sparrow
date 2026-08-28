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
}

enum class ImageVideoTypeDto {
    IMAGE,
    VIDEO
}
