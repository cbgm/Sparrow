package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservePendingDirectInvitationsUseCase(
    private val observeDirectInvitations: ObserveDirectInvitationsUseCase,
    private val modeRepository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(): Flow<List<Invitation>> =
        combine(
            observeDirectInvitations(InvitationDirection.INCOMING),
            modeRepository.observeMode()
        ) { invitations, mode ->
            if (mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                invitations.filter { invitation ->
                    invitation.status == InvitationStatus.PENDING
                }
            } else {
                emptyList()
            }
        }
}
