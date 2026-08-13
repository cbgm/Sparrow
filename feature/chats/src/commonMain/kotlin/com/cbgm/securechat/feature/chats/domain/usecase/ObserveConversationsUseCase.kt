package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.Flow

class ObserveConversationsUseCase(
    private val repository: ChatsRepository
) {
    operator fun invoke(): Flow<List<Conversation>> = repository.observeConversations()
}
