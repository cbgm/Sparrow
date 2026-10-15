package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationReadRepository
import kotlinx.coroutines.flow.Flow

class ObserveDirectConversationUseCase(
    private val repository: DirectConversationReadRepository
) {
    operator fun invoke(conversationId: String): Flow<DirectConversation?> = repository.observe(conversationId)
}
