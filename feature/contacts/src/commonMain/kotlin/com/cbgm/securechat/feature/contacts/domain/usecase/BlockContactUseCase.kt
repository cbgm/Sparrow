package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.securechat.core.security.ContactBlocklistRepository
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.repository.IdentityInvitationRepository

class BlockContactUseCase(
    private val blocklistRepository: ContactBlocklistRepository,
    private val contactRepository: ContactRepository,
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle =
        NoOpMailboxCapabilityLifecycle
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            blocklistRepository.block(contactId)
            val authorizationError =
                identityInvitationRepository.revokeDirectChatAuthorization(contactId).exceptionOrNull()
            val mailboxError = mailboxCapabilityLifecycle.revokeForContact(contactId).exceptionOrNull()
            authorizationError?.let { throw it }
            mailboxError?.let { throw it }
        }

    suspend fun byPhoneNumber(phoneNumber: String): Result<Unit> =
        runCatching {
            val contact = contactRepository.findOrCreateByPhoneNumber(phoneNumber).getOrThrow()
            invoke(contact.id).getOrThrow()
        }
}
