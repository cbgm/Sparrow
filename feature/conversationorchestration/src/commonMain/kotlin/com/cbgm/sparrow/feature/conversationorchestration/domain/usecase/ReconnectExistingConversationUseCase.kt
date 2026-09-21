package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

/** Explicitly request a new invitation while retaining a surviving direct conversation. */
class ReconnectExistingConversationUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(peerId: String): Result<Unit> =
        flowHandler.startExplicitReconnection(peerId)
}
