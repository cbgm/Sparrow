package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class DeletePeerConversationUseCase(
    private val conversationPort: ConversationPort,
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> =
        runCatching {
            val peerId =
                conversationPort
                    .findPeerId(conversationId)
                    .getOrThrow()
                    ?: return@runCatching

            directIdentityExchangeRepository
                .revokeDirectChatAuthorization(peerId)
                .getOrThrow()
            mailboxCapabilityLifecycle
                .revokeForContact(peerId)
                .getOrThrow()
            conversationPort
                .deleteConversation(conversationId)
                .getOrThrow()
        }
}
