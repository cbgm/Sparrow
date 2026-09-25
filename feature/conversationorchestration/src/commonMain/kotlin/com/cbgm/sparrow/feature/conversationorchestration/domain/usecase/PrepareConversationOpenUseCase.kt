package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

class PrepareConversationOpenUseCase internal constructor(
    private val conversationPort: ConversationPort,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val flowHandler: ConversationFlowHandler
) {
    suspend operator fun invoke(peerId: String): Result<String?> =
        safeSuspendCall {
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }

            // Authorization alone is not evidence that a locally deleted chat still
            // exists. Reopening a missing chat must go through an explicit invitation;
            // otherwise the other device may still be waiting for authorization.
            val existingConversationId = conversationPort.findConversationId(peerId).getOrThrow()
            if (existingConversationId != null && requireDirectChatAuthorization(peerId).isSuccess) {
                existingConversationId
            } else {
                flowHandler.startDirectInvitation(peerId).getOrThrow()
                null
            }
        }
}
