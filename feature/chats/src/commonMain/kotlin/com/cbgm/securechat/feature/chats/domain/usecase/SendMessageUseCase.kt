package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class SendMessageUseCase(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        text: String
    ) {
        repository.sendMessage(conversationId, text)
    }
}
