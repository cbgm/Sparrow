package com.cbgm.sparrow.feature.contacts.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObservePendingContactInvitationCountUseCase(
    private val observePendingContactInvitations: ObservePendingContactInvitationsUseCase
) {
    operator fun invoke(): Flow<Int> =
        observePendingContactInvitations()
            .map { invitations -> invitations.size }
            .distinctUntilChanged()
}
