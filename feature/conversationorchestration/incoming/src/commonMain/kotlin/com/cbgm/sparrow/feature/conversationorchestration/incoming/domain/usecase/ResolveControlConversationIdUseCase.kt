package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository

/**
 * The conversation identity and invitation packets are handled in: the contact's chat, or an ID derived from the
 * packet when there is none yet. No chat is created for them.
 */
class ResolveControlConversationIdUseCase(
    private val directConversationRepository: DirectConversationRepository
) {
    suspend operator fun invoke(contactId: String, packetId: String): Result<String> =
        safeSuspendCall(logFailure = false) {
            directConversationRepository.findConversationId(contactId).getOrThrow() ?: "control-$packetId"
        }
}
