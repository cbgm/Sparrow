package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

/**
 * Explicit peer-initiated invitation after the user approved a replacement identity.
 * The existing direct conversation is not deleted or recreated: only the invitation
 * is started. Identity handles key exchange; Invite handles the invitation lifecycle.
 */
class StartRecoveryInvitationUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(peerId: String): Result<Unit> =
        flowHandler.startDirectInvitation(peerId)
}
