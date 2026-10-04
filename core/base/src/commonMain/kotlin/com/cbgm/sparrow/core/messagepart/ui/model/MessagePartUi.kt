package com.cbgm.sparrow.core.messagepart.ui.model

sealed interface MessagePartUi {
    val id: String
}

data class TextUi(
    override val id: String,
    val text: String,
    val source: MessagePartSourceUi = MessagePartSourceUi.Message,
    val isContentFailed: Boolean = false
) : MessagePartUi

data class ImageUi(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val fileName: String? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null,
    val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class VideoUi(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null,
    val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class FileUi(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String,
    val localFilePath: String? = null,
    val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class VoiceUi(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val durationMilliseconds: Long,
    val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class LocationUi(
    override val id: String,
    val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class ContactUi(
    override val id: String,
    val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class PollUi(
    override val id: String,
    val question: String,
    val description: String? = null,
    val options: List<PollOptionUi> = emptyList(),
    val images: List<ImageUi> = emptyList(),
    val allowMultipleSelection: Boolean = false,
    val allowVoteChange: Boolean = true,
    val isAnonymous: Boolean = false,
    val expiresAtEpochMilliseconds: Long? = null,
    val closedAtEpochMilliseconds: Long? = null,
    val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class PollOptionUi(
    val id: String,
    val text: String
)

sealed interface MessagePartSourceUi {
    data object Message : MessagePartSourceUi

    data class GroupPin(
        val groupId: String
    ) : MessagePartSourceUi {
        init {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
        }
    }
}
