package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveContactUseCase(
    private val repository: ContactRepository
) {
    operator fun invoke(contactId: String): Flow<Contact?> =
        repository
            .observeContacts()
            .map { contacts ->
                contacts.firstOrNull { contact -> contact.id == contactId }
            }.distinctUntilChanged()
}
