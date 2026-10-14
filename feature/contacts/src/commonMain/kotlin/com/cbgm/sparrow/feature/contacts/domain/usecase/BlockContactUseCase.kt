package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.protocol.mailbox.NoOpMailboxCapabilityLifecycle

class BlockContactUseCase(
    private val blocklistRepository: ContactBlocklistRepository,
    private val contactRepository: ContactRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle = NoOpMailboxCapabilityLifecycle
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            blocklistRepository.block(contactId).getOrThrow()
            val mailboxError = mailboxCapabilityLifecycle.revokeForContact(contactId).exceptionOrNull()
            mailboxError?.let { throw it }
        }

    suspend fun byPhoneNumber(phoneNumber: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val contact = contactRepository.findOrCreateByPhoneNumber(phoneNumber).getOrThrow()
            invoke(contact.id).getOrThrow()
        }
}
