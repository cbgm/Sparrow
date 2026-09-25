package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

class PromoteConversationGroupMemberUseCase internal constructor(
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String
    ): Result<Unit> = flowHandler.promoteGroupMember(groupId, contactId)
}
