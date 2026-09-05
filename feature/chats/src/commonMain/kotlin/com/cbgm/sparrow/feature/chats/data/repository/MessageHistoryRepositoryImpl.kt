package com.cbgm.sparrow.feature.chats.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.model.MessageCursorDto
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.repository.MessageHistoryRepository

class MessageHistoryRepositoryImpl(
    private val chatDao: ChatDao
) : MessageHistoryRepository {
    override suspend fun findRecentCursors(
        conversationId: String,
        limit: Int
    ): Result<List<MessageHistoryCursor>> =
        safeSuspendCall {
            chatDao.findRecentMessageCursors(conversationId, limit).map(MessageCursorDto::toHistoryCursor)
        }

    override suspend fun findCursorsBefore(
        conversationId: String,
        before: MessageHistoryCursor,
        limit: Int
    ): Result<List<MessageHistoryCursor>> =
        safeSuspendCall {
            chatDao.findMessageCursorsBefore(
                conversationId = conversationId,
                beforeTimestamp = before.createdAtEpochMilliseconds,
                beforeMessageId = before.messageId,
                limit = limit
            ).map(MessageCursorDto::toHistoryCursor)
        }

    override suspend fun findCursor(
        conversationId: String,
        messageId: String
    ): Result<MessageHistoryCursor?> =
        safeSuspendCall {
            chatDao
                .findMessageCursor(conversationId, messageId)
                ?.toHistoryCursor()
        }
}

private fun MessageCursorDto.toHistoryCursor(): MessageHistoryCursor =
    MessageHistoryCursor(
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        messageId = id
    )
