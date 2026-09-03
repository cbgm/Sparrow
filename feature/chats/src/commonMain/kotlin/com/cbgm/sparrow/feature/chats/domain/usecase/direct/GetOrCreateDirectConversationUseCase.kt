package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository

class GetOrCreateDirectConversationUseCase(
    private val repository: DirectConversationRepository
) {
    suspend operator fun invoke(contactId: String): Result<String> = repository.getOrCreate(contactId)
}
