package com.cbgm.securechat.feature.chats.domain.usecase.direct

import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectMessageRepository

class MarkDirectConversationReadUseCase(
    private val repository: DirectMessageRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> = repository.markConversationRead(conversationId)
}
