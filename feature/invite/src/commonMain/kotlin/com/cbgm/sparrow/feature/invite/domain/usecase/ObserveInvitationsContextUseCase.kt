package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationsContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveInvitationsContextUseCase(
    private val observeInvitations: ObserveInvitationsUseCase
) {
    operator fun invoke(): Flow<InvitationsContext> =
        combine(
            observeInvitations(InvitationDirection.INCOMING),
            observeInvitations(InvitationDirection.OUTGOING)
        ) { incoming, outgoing ->
            InvitationsContext(
                incoming = incoming,
                outgoing = outgoing
            )
        }
}
