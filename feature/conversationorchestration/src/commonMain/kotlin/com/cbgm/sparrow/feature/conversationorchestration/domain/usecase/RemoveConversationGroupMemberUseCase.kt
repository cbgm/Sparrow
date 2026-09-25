package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

class RemoveConversationGroupMemberUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String
    ): Result<Unit> = flowHandler.removeGroupMember(groupId, contactId)
}
