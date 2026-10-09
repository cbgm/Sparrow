package com.cbgm.sparrow.core.messagepart.ui.model

sealed interface MessagePartUi {
    val id: String
    val source: MessagePartSourceUi

    val instanceKey: String
        get() =
            when (val partSource = source) {
                MessagePartSourceUi.Message -> "message:$id"
                is MessagePartSourceUi.GroupPin -> "group-pin:${partSource.groupId}:$id"
            }
}

data class TextUi(
    override val id: String,
    val text: String,
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message,
    val isContentFailed: Boolean = false
) : MessagePartUi

sealed interface ImageVideoUi : MessagePartUi

data class ImageUi(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val fileName: String? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null,
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : ImageVideoUi

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
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : ImageVideoUi

data class FileUi(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String,
    val localFilePath: String? = null,
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class VoiceUi(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val durationMilliseconds: Long,
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class LocationUi(
    override val id: String,
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class ContactUi(
    override val id: String,
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
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
    val canClose: Boolean = false,
    val voterDisplayNames: Map<String, String> = emptyMap(),
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class PollOptionUi(
    val id: String,
    val text: String,
    val voterIds: Set<String> = emptySet()
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

data class ExpenseBoardUi(
    override val id: String,
    val currencyCode: String,
    val activatedAtEpochMilliseconds: Long,
    val closedAtEpochMilliseconds: Long? = null,
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class ExpenseUi(
    override val id: String,
    val boardId: String,
    val description: String,
    val amountMinor: Long,
    val currencyCode: String,
    val paidByMemberId: String,
    val allocations: List<ExpenseAllocationUi>,
    val occurredAtEpochMilliseconds: Long,
    val receipt: ImageUi? = null,
    val category: String = "OTHER",
    override val source: MessagePartSourceUi = MessagePartSourceUi.Message
) : MessagePartUi

data class ExpenseAllocationUi(
    val memberId: String,
    val amountMinor: Long
)
