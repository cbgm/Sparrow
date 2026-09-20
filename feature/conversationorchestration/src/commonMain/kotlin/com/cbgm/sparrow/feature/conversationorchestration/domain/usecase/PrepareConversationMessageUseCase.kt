package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.conversationorchestration.domain.model.ConversationMessagePlan
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.usecase.ObserveInvitationLifecycleStatusUseCase
import kotlinx.coroutines.flow.first

class PrepareConversationMessageUseCase internal constructor(
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val flowHandler: ConversationFlowHandler,
    private val observeInvitationLifecycleStatus: ObserveInvitationLifecycleStatusUseCase,
    private val observeIdentityHandshakeState: ObserveIdentityHandshakeStateUseCase,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(peerId: String): Result<ConversationMessagePlan> =
        safeSuspendCall {
            require(peerId.isNotBlank()) {
                "Peer ID must not be blank"
            }

            if (contactBlocklistRepository.isBlocked(peerId)) {
                throw DirectChatAuthorizationRequiredException(
                    "Blocked contacts cannot send or receive direct messages"
                )
            }

            val authorizationError =
                requireDirectChatAuthorization(peerId)
                    .exceptionOrNull()
                    ?: return@safeSuspendCall ConversationMessagePlan.Send

            if (authorizationError !is DirectChatAuthorizationRequiredException) {
                throw authorizationError
            }

            when (identitySetupModeRepository.getMode()) {
                DirectIdentitySetupMode.AUTOMATIC_INVITATION -> {
                    val invitationStatus =
                        observeInvitationLifecycleStatus(
                            payloadType = InvitationPayloadType.DIRECT,
                            payloadId = peerId,
                            peerId = peerId,
                            direction = InvitationDirection.OUTGOING
                        ).first()
                    val handshake = observeIdentityHandshakeState(peerId).first()

                    when {
                        invitationStatus == InvitationLifecycleStatus.PENDING ->
                            ConversationMessagePlan.Queue

                        invitationStatus in RETRYABLE_INVITATION_STATUSES ||
                            handshake in RETRYABLE_IDENTITY_STATES ||
                            handshake == null ->
                            flowHandler.startDirectInvitation(peerId).fold(
                                onSuccess = { ConversationMessagePlan.Queue },
                                onFailure = { error ->
                                    ConversationMessagePlan.QueueWithAuthorizationFailure(error)
                                }
                            )

                        else -> throw authorizationError
                    }
                }

                // Never send an IdentityPacket as a side effect of Send in manual mode.
                // The explicit setup action is the only operation that starts sharing.
                DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
                    ConversationMessagePlan.Queue
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
