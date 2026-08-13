package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class RetryMessageUseCase(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(
        messageId: String
    ): Result<Unit> = repository.retryMessage(messageId)
}
