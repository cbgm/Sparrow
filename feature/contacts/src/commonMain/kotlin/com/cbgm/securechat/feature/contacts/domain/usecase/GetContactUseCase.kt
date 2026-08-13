package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository

class GetContactUseCase(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(contactId: String): Result<Contact?> = repository.getContact(contactId = contactId)
}
