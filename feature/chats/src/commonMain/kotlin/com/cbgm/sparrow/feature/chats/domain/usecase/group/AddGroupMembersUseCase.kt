package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.AddConversationMembersUseCase

class AddGroupMembersUseCase(
    private val addConversationMembers: AddConversationMembersUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> =
        addConversationMembers(
            conversationId = groupId,
            peerIds = contactIds
        )
}
