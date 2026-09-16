package com.cbgm.sparrow.feature.contacts.domain

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicy
import com.cbgm.sparrow.feature.invite.domain.repository.DirectIdentityExchangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ContactInvitationPolicy(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
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
        ) { currentInvitations, blockedContactIds ->
            if (direction == InvitationDirection.INCOMING) {
                currentInvitations.filterNot { invitation -> invitation.peerId in blockedContactIds }
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
            check(modeRepository.getMode() == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                "Automatic identity invitations are disabled"
            }

            val contactId =
                directIdentityExchangeRepository
                    .getContactId(invitationId)
                    .getOrThrow()

            check(!contactBlocklistRepository.isBlocked(contactId)) {
                "Blocked contacts cannot be accepted"
            }
        }
}
