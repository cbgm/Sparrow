package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveContactInvitationsUseCase(
    private val repository: IdentityInvitationRepository
) {
    operator fun invoke(direction: IdentityInvitationDirection): Flow<List<ContactInvitation>> =
        repository.observeInvitations(direction)
}
