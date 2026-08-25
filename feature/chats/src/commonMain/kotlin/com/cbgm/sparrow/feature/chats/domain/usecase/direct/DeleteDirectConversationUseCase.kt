package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectConversationRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class DeleteDirectConversationUseCase(
    private val conversationRepository: DirectConversationRepository,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle,
    private val messageAttachmentRepository: MessageAttachmentRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> =
        runCatching {
            val contactId = conversationRepository.findContactId(conversationId).getOrThrow()
                ?: return@runCatching

            identityInvitationRepository
                .revokeDirectChatAuthorization(contactId)
                .getOrThrow()
            mailboxCapabilityLifecycle
                .revokeForContact(contactId)
                .getOrThrow()
            messageAttachmentRepository.deleteLocalAttachmentsForConversation(conversationId).getOrThrow()
            conversationRepository.delete(conversationId).getOrThrow()
        }
}
