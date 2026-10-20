package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectInvitationState
import com.cbgm.sparrow.feature.identity.domain.model.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationLifecycleStatusUseCase
import kotlinx.coroutines.flow.first

class InspectDirectInvitationUseCase(
    private val observeInvitationLifecycleStatus: ObserveInvitationLifecycleStatusUseCase,
    private val observeIdentityHandshakeState: ObserveIdentityHandshakeStateUseCase,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository
) {
    suspend operator fun invoke(contactId: String): Result<DirectInvitationState> =
        safeSuspendCall(logFailure = false) {
            val invitationStatus =
                observeInvitationLifecycleStatus(
                    payloadType = InvitationPayloadType.DIRECT,
                    payloadId = contactId,
                    peerId = contactId,
                    direction = InvitationDirection.OUTGOING
                ).first()
            if (invitationStatus == InvitationLifecycleStatus.PENDING) {
                return@safeSuspendCall DirectInvitationState.PENDING
            }

            when (identitySetupModeRepository.getMode().getOrThrow()) {
                DirectIdentitySetupMode.AUTOMATIC_INVITATION -> {
                    val handshake = observeIdentityHandshakeState(contactId).first()
                    if (invitationStatus in RETRYABLE_INVITATION_STATUSES ||
                        handshake in RETRYABLE_IDENTITY_STATES ||
                        handshake == null
                    ) {
                        DirectInvitationState.RETRYABLE
                    } else {
                        DirectInvitationState.UNAVAILABLE
                    }
                }

                // Sending a message is an explicit user action and may start a SIGNED
                // INVITATION in manual mode, too. The invite advertises autoSharesIdentity=false:
                // it does not silently import keys or send an IdentityPacket.
                DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING -> DirectInvitationState.RETRYABLE
            }
        }

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
