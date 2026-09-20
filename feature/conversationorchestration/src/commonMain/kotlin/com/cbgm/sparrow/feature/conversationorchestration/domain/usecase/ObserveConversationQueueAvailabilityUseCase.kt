package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

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
    private val observeIdentityHandshakeState: ObserveIdentityHandshakeStateUseCase
) {
    operator fun invoke(peerId: String): Flow<Boolean> =
        combine(
            observeInvitationLifecycleStatus(
                payloadType = InvitationPayloadType.DIRECT,
                payloadId = peerId,
                peerId = peerId,
                direction = InvitationDirection.OUTGOING
            ),
            observeIdentityHandshakeState(peerId)
        ) { invitationStatus, handshake ->
            invitationStatus == InvitationLifecycleStatus.PENDING ||
                invitationStatus in RETRYABLE_INVITATION_STATUSES ||
                handshake in RETRYABLE_IDENTITY_STATES ||
                handshake == null
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
