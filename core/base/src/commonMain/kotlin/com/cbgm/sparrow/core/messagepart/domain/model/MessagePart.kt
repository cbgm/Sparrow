package com.cbgm.sparrow.core.messagepart.domain.model

sealed interface MessagePart {
    val id: String
}

data class Text(
    override val id: String,
    val text: String
) : MessagePart

data class Image(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val fileName: String? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null
) : MessagePart

data class Video(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null
) : MessagePart

data class File(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String,
    val localFilePath: String? = null
) : MessagePart

data class Voice(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val durationMilliseconds: Long
) : MessagePart

data class Location(
    override val id: String
) : MessagePart

data class Contact(
    override val id: String
) : MessagePart

data class Poll(
    override val id: String,
    val question: String,
    val description: String? = null,
    val options: List<PollOption> = emptyList(),
    val images: List<Image> = emptyList(),
    val allowMultipleSelection: Boolean = false,
    val allowVoteChange: Boolean = true,
    val isAnonymous: Boolean = false,
    val expiresAtEpochMilliseconds: Long? = null,
    val closedAtEpochMilliseconds: Long? = null
) : MessagePart

data class PollOption(
    val id: String,
    val text: String
)

sealed interface MessagePartSource {
    data object Message : MessagePartSource

    data class GroupPin(
        val groupId: String
    ) : MessagePartSource {
        init {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
        }
    }
}
