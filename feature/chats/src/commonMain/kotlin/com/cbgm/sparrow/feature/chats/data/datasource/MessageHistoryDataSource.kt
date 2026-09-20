package com.cbgm.sparrow.feature.chats.data.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.model.MessageCursorDto

/** Chats-owned message history access; the repository maps database cursors into domain cursors. */
internal class MessageHistoryDataSource(
    private val chatDao: ChatDao
) {
    suspend fun findRecentCursors(conversationId: String, limit: Int): List<MessageCursorDto> =
        chatDao.findRecentMessageCursors(conversationId, limit)

    suspend fun findCursorsBefore(
        conversationId: String,
        beforeTimestamp: Long,
        beforeMessageId: String,
        limit: Int
    ): List<MessageCursorDto> =
        chatDao.findMessageCursorsBefore(conversationId, beforeTimestamp, beforeMessageId, limit)

    suspend fun findCursor(conversationId: String, messageId: String): MessageCursorDto? =
        chatDao.findMessageCursor(conversationId, messageId)
}
