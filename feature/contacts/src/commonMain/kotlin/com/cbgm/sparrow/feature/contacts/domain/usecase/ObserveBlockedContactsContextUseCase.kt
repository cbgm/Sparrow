package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.BlockedContactsContext
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveBlockedContactsContextUseCase(
    private val observeContactBlocklist: ObserveContactBlocklistUseCase,
    private val observeProfilePictures: ObserveContactProfilePicturesUseCase
) {
    operator fun invoke(): Flow<BlockedContactsContext> =
        observeContactBlocklist()
            .flatMapLatest { blocklist ->
                val contacts = blocklist.blockedContacts + blocklist.availableContacts
                observeProfilePictures(contacts.mapTo(mutableSetOf(), Contact::id))
                    .map { profilePictures ->
                        BlockedContactsContext(
                            blocklist = blocklist,
                            profilePictures = profilePictures
                        )
                    }
            }
}
