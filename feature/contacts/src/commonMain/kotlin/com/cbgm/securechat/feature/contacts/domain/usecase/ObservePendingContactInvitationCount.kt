package com.cbgm.securechat.feature.contacts.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObservePendingContactInvitationCount(
    private val observePendingContactInvitations: ObservePendingContactInvitations
) {
    operator fun invoke(): Flow<Int> =
        observePendingContactInvitations()
            .map { invitations -> invitations.size }
            .distinctUntilChanged()
}
