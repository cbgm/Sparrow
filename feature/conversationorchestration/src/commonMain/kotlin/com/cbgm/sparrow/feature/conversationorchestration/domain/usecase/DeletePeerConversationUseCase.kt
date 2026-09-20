package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

class DeletePeerConversationUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> =
        flowHandler.deletePeerConversation(conversationId)
}
