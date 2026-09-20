package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveRemoteIdentitiesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Composes owner-provided read streams without leaking Identity's DAO into Contacts. */
class ObserveContactsUseCase(
    private val repository: ContactRepository,
    private val observeRemoteIdentities: ObserveRemoteIdentitiesUseCase
) {
    operator fun invoke(): Flow<List<Contact>> =
        combine(repository.observeContacts(), observeRemoteIdentities()) { contacts, identities ->
            val identitiesByPeerId = identities.associateBy { identity -> identity.peerId }
            contacts.map { contact -> contact.withRemoteIdentity(identitiesByPeerId[contact.id]) }
        }.distinctUntilChanged()
}
