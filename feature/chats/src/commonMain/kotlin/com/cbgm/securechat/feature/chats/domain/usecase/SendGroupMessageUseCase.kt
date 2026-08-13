package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class SendGroupMessageUseCase(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        text: String
    ): Result<Unit> = repository.sendGroupMessage(conversationId, text)
}
