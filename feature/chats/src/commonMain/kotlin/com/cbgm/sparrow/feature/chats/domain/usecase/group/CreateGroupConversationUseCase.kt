package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.CreateConversationGroupUseCase

class CreateGroupConversationUseCase(
    private val createConversationGroup: CreateConversationGroupUseCase
) {
    suspend operator fun invoke(title: String, contactIds: Set<String>): Result<String> =
        createConversationGroup(title, contactIds)
}
