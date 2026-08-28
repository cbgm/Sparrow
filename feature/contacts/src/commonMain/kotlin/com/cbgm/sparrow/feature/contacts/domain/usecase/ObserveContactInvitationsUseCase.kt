package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveContactInvitationsUseCase(
    private val repository: IdentityInvitationRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    operator fun invoke(direction: IdentityInvitationDirection): Flow<List<ContactInvitation>> =
        combine(
            repository.observeInvitations(direction),
            contactBlocklistRepository.observeBlockedContactIds()
        ) { invitations, blockedContactIds ->
            if (direction == IdentityInvitationDirection.INCOMING) {
                invitations.filterNot { invitation -> invitation.contactId in blockedContactIds }
            } else {
                invitations
            }
        }
}
