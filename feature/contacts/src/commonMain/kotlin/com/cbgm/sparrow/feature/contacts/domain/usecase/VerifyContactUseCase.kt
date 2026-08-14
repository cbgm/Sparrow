package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository

class VerifyContactUseCase(
    private val repository: ContactRepository,
    private val contactVerificationRepository: ContactVerificationRepository
) {
    suspend operator fun invoke(contactId: String): Result<Contact> =
        runCatching {
            contactVerificationRepository.verify(contactId = contactId).getOrThrow()
            repository.getContact(contactId = contactId).getOrThrow() ?: error("Contact not found: $contactId")
        }
}
