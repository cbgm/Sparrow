package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.ContactBlocklist
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import kotlinx.coroutines.flow.combine

class ObserveContactBlocklistUseCase(
    private val observeContacts: ObserveContactsUseCase,
    private val repository: ContactBlocklistRepository
) {
    operator fun invoke() =
        combine(
            observeContacts(),
            repository.observeBlockedContactIds()
        ) { contacts, blockedContactIds ->
            ContactBlocklist(
                blockedContacts = contacts.filter { contact -> contact.id in blockedContactIds },
                availableContacts = contacts.filterNot { contact -> contact.id in blockedContactIds }
            )
        }
}
