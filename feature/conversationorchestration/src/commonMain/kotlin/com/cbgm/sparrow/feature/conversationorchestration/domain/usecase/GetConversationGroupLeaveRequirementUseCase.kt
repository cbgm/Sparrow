package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement

class GetConversationGroupLeaveRequirementUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(groupId: String): Result<GroupLeaveRequirement> =
        flowHandler.getGroupLeaveRequirement(groupId)
}
