package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository

class GetContactUseCase(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(contactId: String): Result<Contact?> = repository.getContact(contactId = contactId)
}
