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

    data class Voice(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val durationMilliseconds: Long
    ) : MessagePart

    data class Poll(
        val id: String,
        val question: String,
        val description: String? = null,
        val options: List<PollOption> = emptyList(),
        val media: List<ImageVideo> = emptyList(),
        val allowMultipleSelection: Boolean = false,
        val allowVoteChange: Boolean = true,
        val isAnonymous: Boolean = false,
        val expiresAtEpochMilliseconds: Long? = null
    ) : MessagePart
}

data class PollOption(
    val id: String,
    val text: String
)

enum class ImageVideoType {
    IMAGE,
    VIDEO
}
