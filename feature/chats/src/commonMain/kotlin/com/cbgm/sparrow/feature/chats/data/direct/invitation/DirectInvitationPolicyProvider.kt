package com.cbgm.sparrow.feature.chats.data.direct.invitation

import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.invite.domain.policy.InvitationPolicyProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DirectInvitationPolicyProvider(
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) : InvitationPolicyProvider {
    override fun observeEnabled(): Flow<Boolean> =
        modeRepository
            .observeMode()
            .map { mode -> mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION }

    override fun observeBlockedPeerIds(): Flow<Set<String>> =
        contactBlocklistRepository.observeBlockedContactIds()

    override suspend fun isEnabled(): Boolean =
        modeRepository.getMode() == DirectIdentitySetupMode.AUTOMATIC_INVITATION

    override suspend fun getBlockedPeerIds(): Set<String> =
        contactBlocklistRepository.getBlockedContactIds()

    override suspend fun blockUnknownPeers(): Boolean =
        contactBlocklistRepository.getBlockUnknownContactInvites()

    override suspend fun isPeerBlocked(peerId: String): Boolean =
        contactBlocklistRepository.isBlocked(peerId)
}
