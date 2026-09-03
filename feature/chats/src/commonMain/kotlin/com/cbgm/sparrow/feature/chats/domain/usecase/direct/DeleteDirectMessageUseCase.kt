package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class DeleteDirectMessageUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(conversationId: String, messageId: String): Result<Unit> =
        repository.deleteMessage(conversationId, messageId)
}
