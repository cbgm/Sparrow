package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.model.GroupConversation
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupConversationUseCase(
    private val repository: ChatsRepository
) {
    operator fun invoke(conversationId: String): Flow<GroupConversation?> = repository.observeGroupConversation(conversationId)
}
