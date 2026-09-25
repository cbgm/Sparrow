package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

class TransferConversationGroupAdminAndLeaveUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String
    ): Result<Unit> = flowHandler.transferGroupAdminAndLeave(groupId, contactId)
}
