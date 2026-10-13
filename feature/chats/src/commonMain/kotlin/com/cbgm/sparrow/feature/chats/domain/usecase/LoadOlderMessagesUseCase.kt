package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryLoadResult
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryPolicy
import com.cbgm.sparrow.feature.chats.domain.repository.MessageHistoryRepository

class LoadOlderMessagesUseCase(
    private val messageHistoryRepository: MessageHistoryRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        currentOldestCursor: MessageHistoryCursor?,
        pageSize: Int = MessageHistoryPolicy.PAGE_SIZE
    ): Result<MessageHistoryLoadResult> =
        safeSuspendCall {
            require(pageSize > 0) { "Message page size must be positive" }

            val anchor =
                currentOldestCursor
                    ?: messageHistoryRepository
                        .findRecentCursors(conversationId, pageSize)
                        .getOrThrow()
                        .lastOrNull()
                    ?: return@safeSuspendCall MessageHistoryLoadResult(
                        oldestCursor = null,
                        hasMore = false
                    )

            val older =
                messageHistoryRepository
                    .findCursorsBefore(
                        conversationId = conversationId,
                        before = anchor,
                        limit = pageSize + 1
                    ).getOrThrow()

            val page = older.take(pageSize)
            MessageHistoryLoadResult(
                oldestCursor = page.lastOrNull() ?: currentOldestCursor,
                hasMore = older.size > pageSize
            )
        }
}
