package com.cbgm.sparrow.feature.chats.domain.repository

import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor

interface MessageHistoryRepository {
    suspend fun findRecentCursors(
        conversationId: String,
        limit: Int
    ): Result<List<MessageHistoryCursor>>

    suspend fun findCursorsBefore(
        conversationId: String,
        before: MessageHistoryCursor,
        limit: Int
    ): Result<List<MessageHistoryCursor>>

    suspend fun findCursor(
        conversationId: String,
        messageId: String
    ): Result<MessageHistoryCursor?>
}
