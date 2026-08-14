package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow

class ObserveContactsUseCase(
    private val repository: ContactRepository
) {
    operator fun invoke(): Flow<List<Contact>> = repository.observeContacts()
}
