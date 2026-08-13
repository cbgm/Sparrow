package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.Flow

class ObserveConversationUseCase(
    private val repository: ChatsRepository
) {
    operator fun invoke(conversationId: String): Flow<Conversation?> = repository.observeConversation(conversationId)
}
