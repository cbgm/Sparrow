package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class ActivateAuthorizedDirectConversationUseCase(
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            conversationRepository.getOrCreate(contactId).getOrThrow()
            messageRepository.releaseWaitingForAuthorization(contactId).getOrThrow()
        }
}
