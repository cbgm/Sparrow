package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.error.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase

class RequireDirectChatAuthorizationUseCase(
    private val getIdentityPeerState: GetIdentityPeerStateUseCase,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            if (contactBlocklistRepository.isBlocked(contactId)) {
                throw DirectChatAuthorizationRequiredException(
                    "Blocked contacts cannot send or receive direct messages"
                )
            }
            val state = getIdentityPeerState(contactId).getOrThrow()
            if (!state.hasEstablishedExchange) {
                throw DirectChatAuthorizationRequiredException(
                    "A contact invitation must be accepted before messages can be sent"
                )
            }
        }
}
