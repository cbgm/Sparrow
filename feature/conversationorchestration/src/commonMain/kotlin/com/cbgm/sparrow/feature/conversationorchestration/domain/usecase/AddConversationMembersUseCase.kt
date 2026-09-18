package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

class AddConversationMembersUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(
        conversationId: String,
        peerIds: Set<String>
    ): Result<Unit> =
        flowHandler.startGroupInvitations(
            groupId = conversationId,
            peerIds = peerIds
        )
}
