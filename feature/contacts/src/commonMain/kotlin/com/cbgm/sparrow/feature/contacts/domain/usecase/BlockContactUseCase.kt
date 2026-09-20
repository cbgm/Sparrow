package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

class BlockContactUseCase(
    private val blocklistRepository: ContactBlocklistRepository,
    private val contactRepository: ContactRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle = NoOpMailboxCapabilityLifecycle
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        runCatching {
            blocklistRepository.block(contactId)
            val mailboxError = mailboxCapabilityLifecycle.revokeForContact(contactId).exceptionOrNull()
            mailboxError?.let { throw it }
        }

    suspend fun byPhoneNumber(phoneNumber: String): Result<Unit> =
        runCatching {
            val contact = contactRepository.findOrCreateByPhoneNumber(phoneNumber).getOrThrow()
            invoke(contact.id).getOrThrow()
        }
}
