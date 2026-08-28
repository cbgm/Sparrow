package com.cbgm.sparrow.feature.chats.domain.model

sealed interface MessagePart {
    data class Text(
        val text: String
    ) : MessagePart

    data class ImageVideo(
        val id: String,
        val type: ImageVideoType,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null,
        val localFilePath: String? = null
    ) : MessagePart

    data class File(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String,
        val localFilePath: String? = null
    ) : MessagePart

    data class Location(
        val id: String
    ) : MessagePart

    data class Contact(
        val id: String
    ) : MessagePart
}

enum class ImageVideoType {
    IMAGE,
    VIDEO
}
