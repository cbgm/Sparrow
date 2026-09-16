package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class DeleteDirectConversationUseCase(
    private val conversationRepository: DirectConversationRepository,
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle,
    private val messageAttachmentRepository: MessageAttachmentRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> =
        runCatching {
            val contactId = conversationRepository.findContactId(conversationId).getOrThrow()
                ?: return@runCatching

            directIdentityExchangeRepository
                .revokeDirectChatAuthorization(contactId)
                .getOrThrow()
            mailboxCapabilityLifecycle
                .revokeForContact(contactId)
                .getOrThrow()
            messageAttachmentRepository.deleteLocalAttachmentsForConversation(conversationId).getOrThrow()
            conversationRepository.delete(conversationId).getOrThrow()
        }
}
