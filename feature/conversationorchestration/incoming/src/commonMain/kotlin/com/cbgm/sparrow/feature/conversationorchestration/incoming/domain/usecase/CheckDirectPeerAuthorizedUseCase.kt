package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase

/**
 * Whether a contact may have a direct chat with incoming messages: only an established identity exchange, that is
 * an accepted invitation, creates the chat. Unlike sending, keys and pending identity changes are not checked here.
 */
class CheckDirectPeerAuthorizedUseCase(
    private val getIdentityPeerState: GetIdentityPeerStateUseCase
) {
    suspend operator fun invoke(contactId: String): Result<Boolean> =
        safeSuspendCall(logFailure = false) { getIdentityPeerState(contactId).getOrThrow().hasEstablishedExchange }
}
