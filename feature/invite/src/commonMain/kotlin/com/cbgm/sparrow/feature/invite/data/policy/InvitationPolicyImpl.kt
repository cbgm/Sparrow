package com.cbgm.sparrow.feature.invite.data.policy

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationReceptionPolicy
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicy
import com.cbgm.sparrow.feature.invite.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class InvitationPolicyImpl(
    private val repository: InvitationRepository,
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) : InvitationPolicy {
    override fun applyVisibility(
        direction: InvitationDirection,
        invitations: Flow<List<Invitation>>
    ): Flow<List<Invitation>> =
        combine(
            invitations,
            contactBlocklistRepository.observeBlockedContactIds()
        ) { currentInvitations, blockedPeerIds ->
            if (direction == InvitationDirection.INCOMING) {
                currentInvitations.filterNot { invitation -> invitation.peerId in blockedPeerIds }
            } else {
                currentInvitations
            }
        }

    override fun observePendingEnabled(): Flow<Boolean> =
        modeRepository
            .observeMode()
            .map { mode -> mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION }

    override suspend fun validateAcceptance(invitationId: String): Result<Unit> =
        runCatching {
            val payloadType = repository.getPayloadType(invitationId).getOrThrow()
            if (payloadType == InvitationPayloadType.DIRECT) {
                check(modeRepository.getMode() == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                    "Automatic invitations are disabled"
                }
            }

            val peerId = repository.getPeerId(invitationId).getOrThrow()
            check(!contactBlocklistRepository.isBlocked(peerId)) {
                "Blocked peers cannot be accepted"
            }
        }

    override suspend fun getReceptionPolicy(): InvitationReceptionPolicy =
        InvitationReceptionPolicy(
            enabled = modeRepository.getMode() == DirectIdentitySetupMode.AUTOMATIC_INVITATION,
            blockedPeerIds = contactBlocklistRepository.getBlockedContactIds(),
            blockUnknownPeers = contactBlocklistRepository.getBlockUnknownContactInvites()
        )
}
