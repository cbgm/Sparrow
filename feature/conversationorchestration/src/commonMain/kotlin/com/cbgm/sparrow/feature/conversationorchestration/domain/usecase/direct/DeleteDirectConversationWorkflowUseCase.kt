package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase.direct

import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.DirectConversationPort
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class DeleteDirectConversationWorkflowUseCase(
    private val conversationPort: DirectConversationPort,
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> =
        runCatching {
            val contactId =
                conversationPort
                    .findContactId(conversationId)
                    .getOrThrow()
                    ?: return@runCatching

            directIdentityExchangeRepository
                .revokeDirectChatAuthorization(contactId)
                .getOrThrow()
            mailboxCapabilityLifecycle
                .revokeForContact(contactId)
                .getOrThrow()
            conversationPort
                .deleteConversation(conversationId)
                .getOrThrow()
        }
}
