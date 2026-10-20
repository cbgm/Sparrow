package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.DirectChatAuthorizationRequiredException

/** Fails with [DirectChatAuthorizationRequiredException] unless direct messages to the contact are authorized. */
class RequireDirectChatAuthorizationUseCase(
    private val contactBlocklistRepository: ContactBlocklistRepository,
    private val findDirectAuthorizationBlock: FindDirectAuthorizationBlockUseCase
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            if (contactBlocklistRepository.isBlocked(contactId).getOrThrow()) {
                throw DirectChatAuthorizationRequiredException(BLOCKED_CONTACT)
            }
            findDirectAuthorizationBlock(contactId).getOrThrow()?.let { reason ->
                throw DirectChatAuthorizationRequiredException(reason)
            }
        }

    private companion object {
        const val BLOCKED_CONTACT = "Blocked contacts cannot send or receive direct messages"
    }
}
