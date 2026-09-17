package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationStatus
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservePendingInvitationsUseCase(
    private val observeInvitations: ObserveInvitationsUseCase,
    private val policy: InvitationPolicy
) {
    operator fun invoke(): Flow<List<Invitation>> =
        combine(
            observeInvitations(InvitationDirection.INCOMING),
            policy.observePendingEnabled()
        ) { invitations, pendingEnabled ->
            invitations.filter { invitation ->
                invitation.status == InvitationStatus.PENDING &&
                    (
                        invitation.payloadType == InvitationPayloadType.GROUP ||
                            pendingEnabled
                    )
            }
        }
}
