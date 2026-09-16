package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationsContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveContactInvitationsContextUseCase(
    private val observeContactInvitations: ObserveContactInvitationsUseCase
) {
    operator fun invoke(): Flow<InvitationsContext> =
        combine(
            observeContactInvitations(InvitationDirection.INCOMING),
            observeContactInvitations(InvitationDirection.OUTGOING)
        ) { incoming, outgoing ->
            InvitationsContext(
                incoming = incoming,
                outgoing = outgoing
            )
        }
}
