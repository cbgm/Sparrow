package com.cbgm.sparrow.feature.chats.domain.usecase.contact

import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand

class ReconnectContactUseCase(
    private val orchestrator: ConversationOrchestrator
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        orchestrator.awaitResult(DirectConversationCommand.ReconnectConversation(contactId)).map { }
}
