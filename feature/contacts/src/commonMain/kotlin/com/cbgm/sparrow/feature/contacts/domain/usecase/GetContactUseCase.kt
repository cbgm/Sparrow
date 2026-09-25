package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase

class GetContactUseCase(
    private val repository: ContactRepository,
    private val getRemoteIdentity: GetRemoteIdentityUseCase
) {
    suspend operator fun invoke(contactId: String): Result<Contact?> = runCatching {
        repository.getContact(contactId).getOrThrow()?.let { contact ->
            contact.withRemoteIdentity(getRemoteIdentity(contact.id).getOrThrow())
        }
    }
}
