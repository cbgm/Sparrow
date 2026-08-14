package com.cbgm.securechat.feature.chats.domain.usecase.direct

import com.cbgm.securechat.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectConversationRepository
import kotlinx.coroutines.flow.Flow

class ObserveDirectConversationUseCase(
    private val repository: DirectConversationRepository
) {
    operator fun invoke(conversationId: String): Flow<DirectConversation?> = repository.observe(conversationId)
}
