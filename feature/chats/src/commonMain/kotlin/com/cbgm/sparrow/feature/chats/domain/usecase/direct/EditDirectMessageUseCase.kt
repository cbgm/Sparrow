package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class EditDirectMessageUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(conversationId: String, messageId: String, text: String): Result<Unit> =
        repository.editMessage(conversationId, messageId, text)
}
