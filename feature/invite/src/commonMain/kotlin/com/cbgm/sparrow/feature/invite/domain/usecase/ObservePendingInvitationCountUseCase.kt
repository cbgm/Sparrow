package com.cbgm.sparrow.feature.invite.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObservePendingInvitationCountUseCase(
    private val observePendingInvitations: ObservePendingInvitationsUseCase
) {
    operator fun invoke(): Flow<Int> =
        observePendingInvitations()
            .map { invitations -> invitations.size }
            .distinctUntilChanged()
}
