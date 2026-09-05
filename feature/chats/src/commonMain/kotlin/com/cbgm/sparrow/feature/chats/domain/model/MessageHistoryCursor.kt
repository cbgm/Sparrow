package com.cbgm.sparrow.feature.chats.domain.model

data class MessageHistoryCursor(
    val createdAtEpochMilliseconds: Long,
    val messageId: String
) {
    fun isOlderThan(other: MessageHistoryCursor): Boolean =
        createdAtEpochMilliseconds < other.createdAtEpochMilliseconds ||
            (createdAtEpochMilliseconds == other.createdAtEpochMilliseconds && messageId < other.messageId)
}
