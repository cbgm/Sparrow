package com.cbgm.securechat.feature.chats.domain.usecase.direct

import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectConversationRepository

class GetOrCreateDirectConversationUseCase(
    private val repository: DirectConversationRepository
) {
    suspend operator fun invoke(contactId: String): String = repository.getOrCreate(contactId)
}
