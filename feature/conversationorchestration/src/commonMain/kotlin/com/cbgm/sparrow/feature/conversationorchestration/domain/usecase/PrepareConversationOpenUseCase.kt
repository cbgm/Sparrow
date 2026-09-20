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

            // Selecting a peer does not create a chat in either setup mode.
            // Only an already authorized exchange may open an existing/new chat.
            if (requireDirectChatAuthorization(peerId).isSuccess) {
                conversationPort.getOrCreateConversation(peerId).getOrThrow()
            } else {
                flowHandler.startDirectInvitation(peerId).getOrThrow()
                null
            }
        }
}
