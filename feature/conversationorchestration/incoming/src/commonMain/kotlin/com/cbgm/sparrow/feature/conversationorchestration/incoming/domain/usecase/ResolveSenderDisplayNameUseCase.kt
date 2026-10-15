package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

/** The name that labels a sender's attachments: contact name, then phone number, then the claimed number. */
class ResolveSenderDisplayNameUseCase(
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(contactId: String, claimedPhoneNumber: String?): Result<String> =
        safeSuspendCall(logFailure = false) {
            contactRepository.getContact(contactId).getOrThrow()?.let { contact ->
                contact.displayName?.takeIf(String::isNotBlank) ?: contact.preferredPhoneNumber?.value
            } ?: claimedPhoneNumber?.takeIf(String::isNotBlank) ?: contactId
        }
}
