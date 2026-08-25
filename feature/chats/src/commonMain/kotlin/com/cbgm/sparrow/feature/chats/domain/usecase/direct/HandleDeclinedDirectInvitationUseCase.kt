package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectMessageRepository

class HandleDeclinedDirectInvitationUseCase(
    private val messageRepository: DirectMessageRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        messageRepository.discardWaitingForAuthorization(contactId)
}
