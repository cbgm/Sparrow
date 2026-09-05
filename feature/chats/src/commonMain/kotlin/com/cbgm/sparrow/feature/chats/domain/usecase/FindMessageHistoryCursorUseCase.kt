package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.repository.MessageHistoryRepository

class FindMessageHistoryCursorUseCase(
    private val messageHistoryRepository: MessageHistoryRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        messageId: String
    ): Result<MessageHistoryCursor?> =
        messageHistoryRepository.findCursor(conversationId, messageId)
}
