package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

/**
 * Automatically initiate a fresh invitation after the user approves a replacement identity.
 * The existing direct conversation is not deleted or recreated: only the invitation
 * is started. Identity handles key exchange; Invite handles the invitation lifecycle.
 */
class StartRecoveryInvitationUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(peerId: String): Result<Unit> =
        flowHandler.requestReauthorization(peerId)
}
