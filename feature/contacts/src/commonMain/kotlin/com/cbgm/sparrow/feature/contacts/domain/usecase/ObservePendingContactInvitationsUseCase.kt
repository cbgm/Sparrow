package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservePendingContactInvitationsUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val modeRepository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(): Flow<List<PendingContactInvitation>> =
        combine(
            identityInvitationRepository.observePendingIncoming(),
            modeRepository.observeMode()
        ) { invitations, mode ->
            if (mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                invitations
            } else {
                emptyList()
            }
        }
}
