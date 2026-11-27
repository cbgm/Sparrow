package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosurePhase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeClosureUseCase

/**
 * A reconnection re-invites the contact of an existing direct chat. A pending invitation from the contact must
 * be reviewed first, because a new outgoing exchange would replace it.
 */
class RequireDirectReconnectAllowedUseCase(
    private val requireContactInvitable: RequireContactInvitableUseCase,
    private val conversationRepository: DirectConversationRepository,
    private val getIdentityExchangeClosure: GetIdentityExchangeClosureUseCase
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            requireContactInvitable(contactId).getOrThrow()
            check(conversationRepository.findConversationId(contactId).getOrThrow() != null) {
                "Reconnection requires an existing direct conversation"
            }
            check(
                getIdentityExchangeClosure(contactId).getOrThrow()?.phase != IdentityExchangeClosurePhase.INCOMING_PENDING
            ) {
                "Review or decline the pending invitation before starting a reconnection"
            }
        }
}
