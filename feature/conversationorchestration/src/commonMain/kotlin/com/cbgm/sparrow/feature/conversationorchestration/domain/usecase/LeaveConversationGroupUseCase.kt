package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

class LeaveConversationGroupUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = flowHandler.leaveGroup(groupId)
}
