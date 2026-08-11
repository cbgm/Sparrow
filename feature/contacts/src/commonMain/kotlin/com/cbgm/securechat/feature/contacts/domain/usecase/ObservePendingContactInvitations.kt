package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.security.DirectIdentitySetupModeRepository
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservePendingContactInvitations(
    private val identityInvitationService: IdentityInvitationService,
    private val modeRepository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(): Flow<List<PendingContactInvitation>> =
        combine(
            identityInvitationService.observePendingIncoming(),
            modeRepository.observeMode()
        ) { invitations, mode ->
            if (mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                invitations
            } else {
                emptyList()
            }
        }
}
