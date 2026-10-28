package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectSendAuthorization

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

    /**
     * Decides how a new message to the contact is handled: a blocked contact is refused, a pending identity
     * change keeps the message waiting, and missing authorization may be repaired by a fresh invitation.
     * The full authorization check runs last and may still report the contact as blocked.
     */
    suspend fun checkForNewMessage(contactId: String): Result<DirectSendAuthorization> =
        safeSuspendCall(logFailure = false) {
            when {
                contactBlocklistRepository.isBlocked(contactId).getOrThrow() ->
                    DirectSendAuthorization.ContactBlocked(BLOCKED_CONTACT)
                findDirectAuthorizationBlock.hasPendingIdentityChange(contactId).getOrThrow() ->
                    DirectSendAuthorization.IdentityChangePending
                else -> authorizationOf(contactId)
            }
        }

    private suspend fun authorizationOf(contactId: String): DirectSendAuthorization {
        val error = invoke(contactId).exceptionOrNull() ?: return DirectSendAuthorization.Granted
        if (error !is DirectChatAuthorizationRequiredException) throw error
        return DirectSendAuthorization.Missing(error)
    }

    private companion object {
        const val BLOCKED_CONTACT = "Blocked contacts cannot send or receive direct messages"
    }
}
