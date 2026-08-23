package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactsWithProfilePictures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveContactsWithProfilePicturesUseCase(
    private val observeContacts: ObserveContactsUseCase,
    private val observeProfilePictures: ObserveContactProfilePicturesUseCase
) {
    operator fun invoke(): Flow<ContactsWithProfilePictures> =
        observeContacts()
            .flatMapLatest { contacts ->
                observeProfilePictures(contacts.mapTo(mutableSetOf(), Contact::id))
                    .map { profilePictures ->
                        ContactsWithProfilePictures(
                            contacts = contacts,
                            profilePictures = profilePictures
                        )
                    }
            }
}
