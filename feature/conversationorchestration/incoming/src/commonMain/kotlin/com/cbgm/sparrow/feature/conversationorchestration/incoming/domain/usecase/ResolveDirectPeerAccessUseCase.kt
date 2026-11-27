package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DirectPeerAccess
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase

/**
 * Whether a direct message or operation of a contact is handled, waits or is dropped. Only an established
 * identity exchange creates the chat (see [CheckDirectPeerAuthorizedUseCase]). While an invitation is still in
 * flight, the peer may already have completed its side and sent a message that overtook the acceptance or
 * READY; such packets wait. Packets of blocked contacts and of contacts without a pending exchange are dropped.
 */
class ResolveDirectPeerAccessUseCase(
    private val getIdentityPeerState: GetIdentityPeerStateUseCase,
    private val blocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(contactId: String): Result<DirectPeerAccess> =
        safeSuspendCall(logFailure = false) {
            val state = getIdentityPeerState(contactId).getOrThrow()
            when {
                state.hasEstablishedExchange -> DirectPeerAccess.AUTHORIZED
                state.hasPendingExchange && !blocklistRepository.isBlocked(contactId).getOrThrow() ->
                    DirectPeerAccess.PENDING
                else -> DirectPeerAccess.DROPPED
            }
        }
}
