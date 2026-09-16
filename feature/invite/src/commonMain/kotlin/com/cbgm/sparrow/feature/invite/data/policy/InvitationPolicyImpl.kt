package com.cbgm.sparrow.feature.invite.data.policy

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationReceptionPolicy
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicy
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicyProvider
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class InvitationPolicyImpl(
    private val repository: InvitationRepository,
    private val provider: InvitationPolicyProvider
) : InvitationPolicy {
    override fun applyVisibility(
        direction: InvitationDirection,
        invitations: Flow<List<Invitation>>
    ): Flow<List<Invitation>> =
        combine(
            invitations,
            provider.observeBlockedPeerIds()
        ) { currentInvitations, blockedPeerIds ->
            if (direction == InvitationDirection.INCOMING) {
                currentInvitations.filterNot { invitation -> invitation.peerId in blockedPeerIds }
            } else {
                currentInvitations
            }
        }

    override fun observePendingEnabled(): Flow<Boolean> = provider.observeEnabled()

    override suspend fun validateAcceptance(invitationId: String): Result<Unit> =
        runCatching {
            check(provider.isEnabled()) {
                "Automatic invitations are disabled"
            }

            val peerId = repository.getPeerId(invitationId).getOrThrow()
            check(!provider.isPeerBlocked(peerId)) {
                "Blocked peers cannot be accepted"
            }
        }

    override suspend fun getReceptionPolicy(): InvitationReceptionPolicy =
        InvitationReceptionPolicy(
            enabled = provider.isEnabled(),
            blockedPeerIds = provider.getBlockedPeerIds(),
            blockUnknownPeers = provider.blockUnknownPeers()
        )
}
