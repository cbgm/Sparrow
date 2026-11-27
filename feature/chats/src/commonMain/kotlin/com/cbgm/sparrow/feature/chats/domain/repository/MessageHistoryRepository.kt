package com.cbgm.sparrow.feature.chats.domain.repository

import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import kotlinx.coroutines.flow.Flow

interface MessageHistoryRepository {
    suspend fun findRecentCursors(
        conversationId: String,
        limit: Int
    ): Result<List<MessageHistoryCursor>>

    /** Every message of the conversation, newest first. */
    fun observeCursors(conversationId: String): Flow<List<MessageHistoryCursor>>

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
