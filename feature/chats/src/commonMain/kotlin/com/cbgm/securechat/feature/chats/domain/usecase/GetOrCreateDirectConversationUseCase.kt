package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class GetOrCreateDirectConversationUseCase(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(contactId: String): String = repository.getOrCreateDirectConversation(contactId)
}
