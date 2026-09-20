package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationLifecycleStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveConversationQueueAvailabilityUseCase(
    private val observeInvitationLifecycleStatus: ObserveInvitationLifecycleStatusUseCase,
    private val observeIdentityHandshakeState: ObserveIdentityHandshakeStateUseCase,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository
) {
    operator fun invoke(peerId: String): Flow<Boolean> =
        combine(
            observeInvitationLifecycleStatus(
                payloadType = InvitationPayloadType.DIRECT,
                payloadId = peerId,
                peerId = peerId,
                direction = InvitationDirection.OUTGOING
            ),
            observeIdentityHandshakeState(peerId),
            identitySetupModeRepository.observeMode()
        ) { invitationStatus, handshake, setupMode ->
            setupMode == DirectIdentitySetupMode.AUTOMATIC_INVITATION &&
                (
                    invitationStatus == InvitationLifecycleStatus.PENDING ||
                        invitationStatus in RETRYABLE_INVITATION_STATUSES ||
                        handshake in RETRYABLE_IDENTITY_STATES ||
                        // Keep in sync with PrepareConversationMessageUseCase: a null
                        // handshake means no recognized state was found at all (e.g. the
                        // peer revoked authorization) and is just as retryable as the
                        // explicitly-mapped states above. Without this, the composer can
                        // land on DISABLED and block typing before a retry ever runs.
                        handshake == null
                )
        }.distinctUntilChanged()

    private companion object {
        val RETRYABLE_INVITATION_STATUSES =
            setOf(
                InvitationLifecycleStatus.DECLINED,
                InvitationLifecycleStatus.EXPIRED,
                InvitationLifecycleStatus.FAILED
            )

        val RETRYABLE_IDENTITY_STATES =
            setOf(
                IdentityHandshakeState.EXCHANGE_INVALIDATED,
                IdentityHandshakeState.FAILED
            )
    }
}
