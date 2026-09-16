package com.cbgm.sparrow.feature.invite.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObservePendingDirectInvitationCountUseCase(
    private val observePendingDirectInvitations: ObservePendingDirectInvitationsUseCase
) {
    operator fun invoke(): Flow<Int> =
        observePendingDirectInvitations()
            .map { invitations -> invitations.size }
            .distinctUntilChanged()
}
