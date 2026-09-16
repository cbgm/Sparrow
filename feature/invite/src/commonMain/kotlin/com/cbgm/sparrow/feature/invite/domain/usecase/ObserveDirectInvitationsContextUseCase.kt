package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationsContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveDirectInvitationsContextUseCase(
    private val observeDirectInvitations: ObserveDirectInvitationsUseCase
) {
    operator fun invoke(): Flow<InvitationsContext> =
        combine(
            observeDirectInvitations(InvitationDirection.INCOMING),
            observeDirectInvitations(InvitationDirection.OUTGOING)
        ) { incoming, outgoing ->
            InvitationsContext(
                incoming = incoming,
                outgoing = outgoing
            )
        }
}
