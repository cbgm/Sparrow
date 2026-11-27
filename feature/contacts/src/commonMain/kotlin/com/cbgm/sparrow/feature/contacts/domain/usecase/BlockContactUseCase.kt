package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityPeerRepository
import com.cbgm.sparrow.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.protocol.mailbox.NoOpMailboxCapabilityLifecycle

class BlockContactUseCase(
    private val blocklistRepository: ContactBlocklistRepository,
    private val contactRepository: ContactRepository,
    private val identityPeerRepository: IdentityPeerRepository,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle = NoOpMailboxCapabilityLifecycle
) {
    suspend operator fun invoke(contactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            blocklistRepository.block(contactId).getOrThrow()
            val mailboxError = mailboxCapabilityLifecycle.revokeForContact(contactId).exceptionOrNull()
            mailboxError?.let { throw it }
        }

    /** Blocks the contact that already has this number in any notation (0151... or +49151...), else a new one. */
    suspend fun byPhoneNumber(phoneNumber: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val contactId =
                identityPeerRepository.findEquivalentPhonePeerId(phoneNumber).getOrThrow()
                    ?: contactRepository.findOrCreateByPhoneNumber(phoneNumber).getOrThrow().id
            invoke(contactId).getOrThrow()
        }
}
