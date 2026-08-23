package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation
import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitationsContext
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveContactInvitationsContextUseCase(
    private val observeContactInvitations: ObserveContactInvitationsUseCase,
    private val observeProfilePictures: ObserveContactProfilePicturesUseCase
) {
    operator fun invoke(): Flow<ContactInvitationsContext> =
        combine(
            observeContactInvitations(IdentityInvitationDirection.INCOMING),
            observeContactInvitations(IdentityInvitationDirection.OUTGOING)
        ) { incoming, outgoing ->
            InvitationsSnapshot(incoming = incoming, outgoing = outgoing)
        }.flatMapLatest { invitations ->
            observeProfilePictures(
                (invitations.incoming + invitations.outgoing)
                    .mapTo(mutableSetOf(), ContactInvitation::contactId)
            ).map { profilePictures ->
                ContactInvitationsContext(
                    incoming = invitations.incoming,
                    outgoing = invitations.outgoing,
                    profilePictures = profilePictures
                )
            }
        }

    private data class InvitationsSnapshot(
        val incoming: List<ContactInvitation>,
        val outgoing: List<ContactInvitation>
    )
}
