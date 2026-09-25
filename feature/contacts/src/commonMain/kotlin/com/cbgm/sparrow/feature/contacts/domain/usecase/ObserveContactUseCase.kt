package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveContactUseCase(
    private val observeContacts: ObserveContactsUseCase
) {
    operator fun invoke(contactId: String): Flow<Contact?> =
        observeContacts().map { contacts ->
            contacts.firstOrNull { contact -> contact.id == contactId }
        }.distinctUntilChanged()
}
