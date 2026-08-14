package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository

class DeleteDirectConversationUseCase(
    private val repository: DirectConversationRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> = repository.delete(conversationId)
}
